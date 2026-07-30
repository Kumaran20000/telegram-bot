document.addEventListener('DOMContentLoaded', async () => {
  const urlInput = document.getElementById('url');
  const titleInput = document.getElementById('title');
  const priceInput = document.getElementById('price');
  const imageInput = document.getElementById('image');
  const saveBtn = document.getElementById('saveBtn');
  const statusDiv = document.getElementById('status');
  const previewCard = document.getElementById('previewCard');
  const previewImg = document.getElementById('previewImg');
  const previewTitle = document.getElementById('previewTitle');
  const previewPrice = document.getElementById('previewPrice');

  const API_BASE = 'http://localhost:8080/api/deals';

  // Automatically fetch current active tab URL & details if on Amazon
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (tab && tab.url) {
      urlInput.value = tab.url;

      if (tab.url.includes('amazon') || tab.url.includes('amzn.')) {
        // Try extracting info from active tab DOM
        chrome.scripting.executeScript({
          target: { tabId: tab.id },
          func: extractAmazonDomDetails
        }, (results) => {
          if (results && results[0] && results[0].result) {
            const data = results[0].result;
            if (data.title) titleInput.value = data.title;
            if (data.price) priceInput.value = data.price;
            if (data.image) imageInput.value = data.image;
            updatePreview(data.title, data.price, data.image);
          } else {
            // Fallback to backend preview API
            fetchPreview(tab.url);
          }
        });
      }
    }
  } catch (e) {
    console.log('Error querying active tab:', e);
  }

  urlInput.addEventListener('change', () => {
    if (urlInput.value) {
      fetchPreview(urlInput.value);
    }
  });

  async function fetchPreview(targetUrl) {
    try {
      const res = await fetch(`${API_BASE}/preview-url?url=${encodeURIComponent(targetUrl)}`);
      if (res.ok) {
        const deal = await res.json();
        if (deal.title && !titleInput.value) titleInput.value = deal.title;
        if (deal.price && !priceInput.value) priceInput.value = deal.price;
        if (deal.image && !imageInput.value) imageInput.value = deal.image;
        updatePreview(deal.title, deal.price, deal.image);
      }
    } catch (e) {
      console.log('Preview error:', e);
    }
  }

  function updatePreview(title, price, image) {
    if (title || price || image) {
      previewCard.style.display = 'flex';
      previewTitle.textContent = title || 'Amazon Product';
      previewPrice.textContent = price ? `₹${price}` : '';
      if (image) previewImg.src = image;
    }
  }

  saveBtn.addEventListener('click', async () => {
    const rawUrl = urlInput.value.trim();
    if (!rawUrl) {
      showStatus('Please enter an Amazon URL or SiteStripe link', false);
      return;
    }

    saveBtn.disabled = true;
    saveBtn.textContent = 'Saving...';
    statusDiv.style.display = 'none';

    try {
      const payload = {
        url: rawUrl,
        title: titleInput.value.trim(),
        price: priceInput.value.trim(),
        image: imageInput.value.trim()
      };

      const res = await fetch(`${API_BASE}/sitestripe`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await res.json();

      if (res.ok && data.status === 'SUCCESS') {
        showStatus('✅ Saved to Google Sheet!', true);
        if (data.deal) {
          updatePreview(data.deal.title, data.deal.price, data.deal.image);
        }
      } else {
        showStatus(`❌ ${data.message || 'Failed to save deal'}`, false);
      }
    } catch (e) {
      showStatus(`❌ Connection error: Ensure backend is running at http://localhost:8080`, false);
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = '🚀 Save to Google Sheet';
    }
  });

  function showStatus(msg, isSuccess) {
    statusDiv.textContent = msg;
    statusDiv.className = isSuccess ? 'success' : 'error';
  }
});

// Function executed inside active tab context
function extractAmazonDomDetails() {
  try {
    const titleEl = document.getElementById('productTitle');
    const title = titleEl ? titleEl.innerText.trim() : document.title;

    let price = '';
    const priceEl = document.querySelector('#corePrice_feature_div .a-offscreen, #corePriceDisplay_desktop_feature_div .a-offscreen, #priceblock_dealprice, #priceblock_ourprice, .a-price .a-offscreen');
    if (priceEl) {
      price = priceEl.innerText.replace(/[^0-9.,]/g, '').trim();
    }

    let image = '';
    const imgEl = document.getElementById('landingImage') || document.getElementById('imgBlkFront');
    if (imgEl) {
      image = imgEl.getAttribute('data-old-hires') || imgEl.src;
    }

    return { title, price, image };
  } catch (e) {
    return null;
  }
}

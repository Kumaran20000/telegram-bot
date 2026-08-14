package com.example.telegram_bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class FacebookConfig {

    @Value("${facebook.page.id:${facebook.page-id:${FACEBOOK_PAGE_ID:}}}")
    private String pageId;

    @Value("${facebook.page.access-token:${facebook.page-access-token:${FACEBOOK_PAGE_ACCESS_TOKEN:}}}")
    private String pageAccessToken;

    @Value("${facebook.enabled:${FACEBOOK_ENABLED:true}}")
    private boolean enabled;

    public String getEffectivePageAccessToken(String fallbackInstagramAccessToken) {
        if (pageAccessToken != null && !pageAccessToken.trim().isEmpty()) {
            return pageAccessToken.trim();
        }
        return fallbackInstagramAccessToken;
    }
}

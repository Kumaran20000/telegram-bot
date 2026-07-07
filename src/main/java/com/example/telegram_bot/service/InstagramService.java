package com.example.telegram_bot.service;

import com.example.telegram_bot.config.InstagramConfig;
import com.example.telegram_bot.model.Deal;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Service;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import org.springframework.web.client.RestTemplate;

import java.util.Map;


@Service
@RequiredArgsConstructor
public class InstagramService {


    private final RestTemplate restTemplate;

    private final InstagramConfig instagramConfig;

    private final CaptionService captionService;



    // Main method called from GoogleSheetService
    public boolean publish(Deal deal) {
        System.out.println("Business ID: " + instagramConfig.getBusinessId());
System.out.println("Access Token: " + instagramConfig.getAccessToken());


        try {

            System.out.println("Starting Instagram upload...");


            // Step 1: Create media container
            String creationId = createMediaContainer(deal);


            if (creationId == null) {

                System.out.println("Failed to create Instagram media container");

                return false;
            }


            System.out.println("Creation ID: " + creationId);



            // Step 2: Publish image
            boolean published = publishMedia(creationId);



            if (published) {

                System.out.println(
                    "Instagram post published successfully"
                );

            } else {

                System.out.println(
                    "Instagram publish failed"
                );

            }


            return published;



        } catch(Exception e) {


            System.out.println(
                "Instagram Error: " + e.getMessage()
            );

            e.printStackTrace();

            return false;

        }

    }





    // Create Instagram media container
    private String createMediaContainer(Deal deal) {


        String url =
                "https://graph.facebook.com/v23.0/"
                + instagramConfig.getBusinessId()
                + "/media";



        MultiValueMap<String,String> body =
                new LinkedMultiValueMap<>();


        body.add(
                "image_url",
                deal.getImage()
        );


        body.add(
                "caption",
                captionService.createCaption(deal)
        );


        body.add(
                "access_token",
                instagramConfig.getAccessToken()
        );



        HttpHeaders headers =
                new HttpHeaders();


        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );



        HttpEntity<MultiValueMap<String,String>> request =
                new HttpEntity<>(
                        body,
                        headers
                );



        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        url,
                        request,
                        Map.class
                );



        Map<String,Object> responseBody =
                response.getBody();



        if(responseBody == null) {

            return null;

        }



        return responseBody
                .get("id")
                .toString();

    }





    // Publish Instagram media
    private boolean publishMedia(String creationId) {



        String url =
                "https://graph.facebook.com/v23.0/"
                + instagramConfig.getBusinessId()
                + "/media_publish";



        MultiValueMap<String,String> body =
                new LinkedMultiValueMap<>();



        body.add(
                "creation_id",
                creationId
        );



        body.add(
                "access_token",
                instagramConfig.getAccessToken()
        );



        HttpHeaders headers =
                new HttpHeaders();


        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );



        HttpEntity<MultiValueMap<String,String>> request =
                new HttpEntity<>(
                        body,
                        headers
                );



        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        url,
                        request,
                        Map.class
                );



        return response
                .getStatusCode()
                .is2xxSuccessful();

    }

}
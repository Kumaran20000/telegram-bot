package com.example.telegram_bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "instagram")
@Getter
@Setter
public class InstagramConfig {

    private String accessToken;

    private String businessId;

}

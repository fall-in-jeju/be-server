package com.jeju.ormicamp.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmap")
public class TmapProperties {

    private String baseUrl;
    private String appKey;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }
}

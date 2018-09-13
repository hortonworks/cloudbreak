package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AmbariRepo {

    private Boolean predefined;

    private String version;

    private String baseUrl;

    private String gpgKeyUrl;

    public Boolean getPredefined() {
        return predefined;
    }

    public void setPredefined(Boolean predefined) {
        this.predefined = predefined;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getGpgKeyUrl() {
        return gpgKeyUrl;
    }

    public void setGpgKeyUrl(String gpgKeyUrl) {
        this.gpgKeyUrl = gpgKeyUrl;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("predefined", predefined);
        map.put("version", version);
        map.put("baseUrl", baseUrl);
        map.put("gpgKeyUrl", gpgKeyUrl);
        return map;
    }
}

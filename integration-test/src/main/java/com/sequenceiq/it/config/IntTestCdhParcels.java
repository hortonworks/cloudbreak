package com.sequenceiq.it.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;

@Component
@ConfigurationProperties("integrationtest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntTestCdhParcels {

    private Map<String, DefaultCDHInfo> cdhParcels = new HashMap<>();

    public Map<String, DefaultCDHInfo> getCdhParcels() {
        return cdhParcels;
    }

    public void setCdhParcels(Map<String, DefaultCDHInfo> cdhParcels) {
        this.cdhParcels = cdhParcels;
    }
}

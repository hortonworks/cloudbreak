package com.sequenceiq.cloudbreak.cloud.model.component;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@ConfigurationProperties("cb.hdp")
@Component
public class DefaultHDPEntries {

    private Map<String, DefaultHDPInfo> entries = new HashMap<>();

    public Map<String, DefaultHDPInfo> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, DefaultHDPInfo> entries) {
        this.entries = entries;
    }
}

package com.sequenceiq.cloudbreak.cloud.model.component;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@ConfigurationProperties("cb.hdf")
@Component
public class DefaultHDFEntries {

    private Map<String, DefaultHDFInfo> entries = new HashMap<>();

    public Map<String, DefaultHDFInfo> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, DefaultHDFInfo> entries) {
        this.entries = entries;
    }
}

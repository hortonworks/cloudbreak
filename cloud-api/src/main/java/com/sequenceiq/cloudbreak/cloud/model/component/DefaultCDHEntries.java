package com.sequenceiq.cloudbreak.cloud.model.component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@ConfigurationProperties("cb.cdh")
@Component
public class DefaultCDHEntries {

    public static final Comparator<Map.Entry<String, DefaultCDHInfo>> CDH_ENTRY_COMPARATOR =
            Comparator.comparing(Map.Entry::getKey);

    private Map<String, DefaultCDHInfo> entries = new HashMap<>();

    public Map<String, DefaultCDHInfo> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, DefaultCDHInfo> entries) {
        this.entries = entries;
    }
}

package com.sequenceiq.cdp.databus.model;

import java.util.HashMap;
import java.util.Map;

public class DatabusRequestContext {

    private final String accountId;

    private final String environmentCrn;

    private final String resourceCrn;

    private final String resourceName;

    private final Map<String, String> additionalDatabusHeaders = new HashMap<>();

    public DatabusRequestContext(String accountId, String environmentCrn, String resourceCrn, String resourceName) {
        this.accountId = accountId;
        this.environmentCrn = environmentCrn;
        this.resourceCrn = resourceCrn;
        this.resourceName = resourceName;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getAccountId() {
        return accountId;
    }

    public Map<String, String> getAdditionalDatabusHeaders() {
        return additionalDatabusHeaders;
    }

    public DatabusRequestContext addAdditionalDatabusHeader(String key, String value) {
        additionalDatabusHeaders.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return "DatabusRequestContext{" +
                "accountId='" + accountId + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", additionalDatabusHeaders=" + additionalDatabusHeaders +
                '}';
    }
}

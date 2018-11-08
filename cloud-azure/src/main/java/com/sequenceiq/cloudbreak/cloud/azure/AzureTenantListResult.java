package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;

public class AzureTenantListResult {

    private String nextLink;

    private List<AzureTenant> value;

    public String getNextLink() {
        return nextLink;
    }

    public List<AzureTenant> getValue() {
        return value;
    }
}

package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;

public class AzureSubscriptionListResult {

    private String nextLink;

    private List<AzureSubscription> value;

    public String getNextLink() {
        return nextLink;
    }

    public List<AzureSubscription> getValue() {
        return value;
    }
}

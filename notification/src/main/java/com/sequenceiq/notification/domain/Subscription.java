package com.sequenceiq.notification.domain;

/**
 * Common abstract class for subscription-related entities (DistributionList and ResourceSubscription).
 * Defines the core fields shared between different subscription types.
 */
public abstract class Subscription {

    private String resourceCrn;

    private String resourceName;

    private String parentResourceCrn;

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getParentResourceCrn() {
        return parentResourceCrn;
    }

    public void setParentResourceCrn(String parentResourceCrn) {
        this.parentResourceCrn = parentResourceCrn;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "resourceCrn='" + resourceCrn + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", parentResourceCrn='" + parentResourceCrn + '\'' +
                '}';
    }
}


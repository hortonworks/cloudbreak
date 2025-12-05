package com.sequenceiq.notification.domain;

/**
 * Represents a resource subscription for notification events.
 */
public class ResourceSubscription extends Subscription {

    private String serviceName;

    private String subscriptionId;

    private String userCrn;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getUserCrn() {
        return userCrn;
    }

    public void setUserCrn(String userCrn) {
        this.userCrn = userCrn;
    }

    public String toString() {
        return super.toString() + " ResourceSubscription{" +
                "serviceName='" + serviceName + '\'' +
                ", subscriptionId='" + subscriptionId + '\'' +
                ", userCrn='" + userCrn + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String resourceCrn;

        private String serviceName;

        private String resourceName;

        private String subscriptionId;

        private String parentResourceCrn;

        private String userCrn;

        public Builder resourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder resourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder subscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
            return this;
        }

        public Builder parentResourceCrn(String parentResourceCrn) {
            this.parentResourceCrn = parentResourceCrn;
            return this;
        }

        public Builder userCrn(String userCrn) {
            this.userCrn = userCrn;
            return this;
        }

        public ResourceSubscription build() {
            ResourceSubscription resourceSubscription = new ResourceSubscription();
            resourceSubscription.setResourceCrn(resourceCrn);
            resourceSubscription.setServiceName(serviceName);
            resourceSubscription.setResourceName(resourceName);
            resourceSubscription.setSubscriptionId(subscriptionId);
            resourceSubscription.setParentResourceCrn(parentResourceCrn);
            resourceSubscription.setUserCrn(userCrn);
            return resourceSubscription;
        }
    }
}

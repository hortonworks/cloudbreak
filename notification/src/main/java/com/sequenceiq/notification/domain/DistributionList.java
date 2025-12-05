package com.sequenceiq.notification.domain;

public class DistributionList extends Subscription {

    private String externalId;

    private DistributionListManagementType type;

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public DistributionListManagementType getType() {
        return type;
    }

    public void setType(DistributionListManagementType type) {
        this.type = type;
    }

    public String generateDistributionListUuid() {
        return String.join("/", getResourceName(), getResourceCrn(), this.externalId);
    }

    public String toString() {
        return super.toString() + " DistributionList{" +
                "externalDistributionListId='" + externalId + '\'' +
                ", type=" + type +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String externalDistributionListId;

        private DistributionListManagementType type;

        private String resourceCrn;

        private String resourceName;

        private String parentResourceCrn;

        public Builder resourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder resourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder parentResourceCrn(String parentResourceCrn) {
            this.parentResourceCrn = parentResourceCrn;
            return this;
        }

        public Builder externalDistributionListId(String externalDistributionListId) {
            this.externalDistributionListId = externalDistributionListId;
            return this;
        }

        public Builder type(DistributionListManagementType type) {
            this.type = type;
            return this;
        }

        public DistributionList build() {
            DistributionList distributionList = new DistributionList();
            distributionList.setResourceCrn(resourceCrn);
            distributionList.setResourceName(resourceName);
            distributionList.setParentResourceCrn(parentResourceCrn);
            distributionList.setExternalId(externalDistributionListId);
            distributionList.setType(type);
            return distributionList;
        }
    }
}

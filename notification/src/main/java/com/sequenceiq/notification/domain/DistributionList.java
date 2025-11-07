package com.sequenceiq.notification.domain;

public class DistributionList {

    private String resourceCrn;

    private String resourceName;

    private String externalDistributionListId;

    private DistributionListManagementType type;

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getExternalDistributionListId() {
        return externalDistributionListId;
    }

    public void setExternalDistributionListId(String externalDistributionListId) {
        this.externalDistributionListId = externalDistributionListId;
    }

    public DistributionListManagementType getType() {
        return type;
    }

    public void setType(DistributionListManagementType type) {
        this.type = type;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String generateDistributionListUuid() {
        return String.join("/", this.resourceName, this.resourceCrn, this.externalDistributionListId);
    }

    @Override
    public String toString() {
        return "DistributionList{" +
                "resourceCrn='" + resourceCrn + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", externalDistributionListId='" + externalDistributionListId + '\'' +
                ", type=" + type +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String resourceCrn;

        private String resourceName;

        private String externalDistributionListId;

        private DistributionListManagementType type;

        public Builder resourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
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

        // Builder method for resourceName
        public Builder resourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public DistributionList build() {
            DistributionList distributionList = new DistributionList();
            distributionList.setResourceCrn(resourceCrn);
            distributionList.setExternalDistributionListId(externalDistributionListId);
            distributionList.setType(type);
            distributionList.setResourceName(resourceName);
            return distributionList;
        }
    }
}

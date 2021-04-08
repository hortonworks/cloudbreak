package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

public class AzureMarketplaceImage {

    private final String publisherId;

    private final String offerId;

    private final String planId;

    private final String version;

    public AzureMarketplaceImage(String publisherId, String offerId, String planId, String version) {
        this.publisherId = publisherId;
        this.offerId = offerId;
        this.planId = planId;
        this.version = version;
    }

    public String getPublisherId() {
        return publisherId;
    }

    public String getOfferId() {
        return offerId;
    }

    public String getPlanId() {
        return planId;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "AzureMarketplaceImage{" +
                "publisherId='" + publisherId + '\'' +
                ", offerId='" + offerId + '\'' +
                ", planId='" + planId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}

package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

public class AzureMarketplaceImage {

    public static final String MARKETPLACE_REGION = "default";

    private final String publisherId;

    private final String offerId;

    private final String planId;

    private final String version;

    public static AzureMarketplaceImage from(String urn) {
        String[] urnParts = urn.split(":");
        if(urnParts.length != 4) {
            throw new BadRequestException(String.format("Hey, urn needs to have 4 parts, and this has %d", urnParts.length));
        }
        return new AzureMarketplaceImage(urnParts[0], urnParts[1], urnParts[2], urnParts[3]);
    }

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

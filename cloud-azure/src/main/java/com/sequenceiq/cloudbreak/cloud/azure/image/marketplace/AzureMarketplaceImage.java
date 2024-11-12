package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

public class AzureMarketplaceImage {

    public static final String MARKETPLACE_REGION = "default";

    private final String publisherId;

    private final String offerId;

    private final String planId;

    private final String version;

    // This is required as for RHEL8 images, the source MP image needs to be specified in the ARM template VM section
    private final boolean usedAsSourceImage;

    public AzureMarketplaceImage(String publisherId, String offerId, String planId, String version, boolean usedAsSourceImage) {
        this.publisherId = publisherId;
        this.offerId = offerId;
        this.planId = planId;
        this.version = version;
        this.usedAsSourceImage = usedAsSourceImage;
    }

    public AzureMarketplaceImage(String publisherId, String offerId, String planId, String version) {
        this(publisherId, offerId, planId, version, false);
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

    public boolean isUsedAsSourceImage() {
        return usedAsSourceImage;
    }

    @Override
    public String toString() {
        return "AzureMarketplaceImage{" +
                "publisherId='" + publisherId + '\'' +
                ", offerId='" + offerId + '\'' +
                ", planId='" + planId + '\'' +
                ", version='" + version + '\'' +
                // We do not want to present this implementation detail for end-users in user facing error messages e.g. for image signing
                (usedAsSourceImage ? (", usedAsSourceImage=" + usedAsSourceImage + '}') : '}');
    }
}
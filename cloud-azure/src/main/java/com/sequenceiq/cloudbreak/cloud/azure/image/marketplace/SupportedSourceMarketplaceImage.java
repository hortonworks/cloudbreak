package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

public enum SupportedSourceMarketplaceImage {

    REDHAT("redhat", "^rhel-byos$"),
    CLOUDERA("cloudera", "^freeipa-(.*)|^cdp-(.*)"),;

    private final String publisher;

    // Offer is a regular expression
    private final String offer;

    SupportedSourceMarketplaceImage(String publisher, String offer) {
        this.publisher = publisher;
        this.offer = offer;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getOffer() {
        return offer;
    }
}
package com.sequenceiq.it.cloudbreak.config.azure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureMarketplaceImageProperties {

    @Value("${integrationtest.azure.marketplace.freeipa.catalog.url}")
    private String catalogUrl;

    @Value("${integrationtest.azure.marketplace.freeipa.image.uuid}")
    private String imageUuid;

    public String getCatalogUrl() {
        return catalogUrl;
    }

    public void setCatalogUrl(String catalogUrl) {
        this.catalogUrl = catalogUrl;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}

package com.sequenceiq.it.cloudbreak.config.azure;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.it.TestParameter;

@Configuration
public class AzureMarketplaceImageProperties {

    public static final String AZURE_MARKETPLACE_FREEIPA_CATALOG_URL = "AZURE_MARKETPLACE_FREEIPA_CATALOG_URL";

    public static final String AZURE_MARKETPLACE_FREEIPA_IMAGE_UUID = "AZURE_MARKETPLACE_FREEIPA_IMAGE_UUID";

    @Value("${integrationtest.azure.marketplace.freeipa.catalog.url}")
    private String catalogUrl;

    @Value("${integrationtest.azure.marketplace.freeipa.image.uuid}")
    private String imageUuid;

    @Inject
    private TestParameter testParameter;

    @PostConstruct
    private void init() {
        testParameter.put(AZURE_MARKETPLACE_FREEIPA_CATALOG_URL, catalogUrl);
        testParameter.put(AZURE_MARKETPLACE_FREEIPA_IMAGE_UUID, imageUuid);
    }
}
package com.sequenceiq.it.cloudbreak.mock;

import java.time.format.DateTimeFormatter;

public class ImageCatalogMockServerSetup {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private String cdhRuntime;

    private String cbVersion;

    private String mockImageCatalogServer;

    public ImageCatalogMockServerSetup(String mockImageCatalogServer, String cbVersion, String cdhRuntime) {
        this.cbVersion = cbVersion;
        this.cdhRuntime = cdhRuntime;
        this.mockImageCatalogServer = mockImageCatalogServer;
    }

    // DYNAMIC address http://localhost:10080/thunderhead/mock-image-catalog?catalog-name=cb-catalog&cb-version=CB-2.29.0&runtime=7.2.2
    public String getFreeIpaImageCatalogUrl() {
        return String.format("http://%s/thunderhead/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s",
                mockImageCatalogServer,
                "freeipa-catalog",
                cbVersion,
                cdhRuntime);
    }

    public String getImageCatalogUrl() {
        return String.format("http://%s/thunderhead/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s",
                mockImageCatalogServer,
                "cb-catalog",
                cbVersion,
                cdhRuntime);
    }

    public String getPreWarmedImageCatalogUrl() {
        return String.format("http://%s/thunderhead/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s",
                mockImageCatalogServer,
                "catalog-with-prewarmed",
                cbVersion,
                cdhRuntime);
    }

    public String getUpgradeImageCatalogUrl() {
        return String.format("http://%s/thunderhead/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s",
                mockImageCatalogServer,
                "catalog-with-for-upgrade",
                cbVersion,
                cdhRuntime);
    }
}

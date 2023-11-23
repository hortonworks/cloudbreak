package com.sequenceiq.it.cloudbreak.mock;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.config.server.ServerProperties;

@Component
@DependsOn("serverProperties")
public class ImageCatalogMockServerSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogMockServerSetup.class);

    private String cdhRuntime;

    private String cbVersion;

    private String mockImageCatalogServer;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private ServerProperties serverProperties;

    @PostConstruct
    void initImageCatalogIfNecessary() {
        mockImageCatalogServer = serverProperties.getMockImageCatalogAddr();
        cbVersion = serverProperties.getCbVersion();
        cdhRuntime = commonClusterManagerProperties.getRuntimeVersion();
    }

    // DYNAMIC address http://localhost:10080/thunderhead/mock-image-catalog?catalog-name=cb-catalog&cb-version=CB-2.29.0&runtime=7.2.2
    public String getFreeIpaImageCatalogUrl() {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "freeipa-catalog",
                cbVersion,
                cdhRuntime,
                mockImageCatalogServer);
    }

    public String getFreeIpaImageCatalogUrlWitdDefaultImageUuid(String defaultImageUuid) {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s&default-image-uuid=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "freeipa-catalog",
                cbVersion,
                cdhRuntime,
                defaultImageUuid,
                mockImageCatalogServer);
    }

    public String getImageCatalogUrl() {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "cb-catalog",
                cbVersion,
                cdhRuntime,
                mockImageCatalogServer);
    }

    public String getPreWarmedImageCatalogUrl() {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "catalog-with-prewarmed",
                cbVersion,
                cdhRuntime,
                mockImageCatalogServer);
    }

    public String getPreWarmedImageCatalogUrlWithDefaultImageUuid(String defaultImageUuid) {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s&default-image-uuid=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "catalog-with-prewarmed",
                cbVersion,
                cdhRuntime,
                defaultImageUuid,
                mockImageCatalogServer);
    }

    public String getPreWarmedImageCatalogUrlWithCmAndCdhVersions(String cmVersion, String cdhVersion) {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s&cm=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "catalog-with-prewarmed",
                cbVersion,
                cdhVersion,
                cmVersion,
                mockImageCatalogServer);
    }

    public String getUpgradeImageCatalogUrl() {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "catalog-with-for-upgrade",
                cbVersion,
                cdhRuntime,
                mockImageCatalogServer);
    }
}

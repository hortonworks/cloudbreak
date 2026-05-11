package com.sequenceiq.it.cloudbreak.mock;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

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

    private String cdhUpgradeRuntime;

    private String cdhRuntime;

    private String mockImageCatalogServer;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private ServerProperties serverProperties;

    @PostConstruct
    void initImageCatalogIfNecessary() {
        mockImageCatalogServer = serverProperties.getMockImageCatalogAddr();
        cdhRuntime = commonClusterManagerProperties.getRuntimeVersion();
        cdhUpgradeRuntime = commonClusterManagerProperties.getUpgrade().getCurrentRuntimeVersion(false);
    }

    // DYNAMIC address http://localhost:10080/thunderhead/mock-image-catalog?catalog-name=cb-catalog&runtime=7.2.2
    public String getFreeIpaImageCatalogUrl() {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&runtime=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "freeipa-catalog",
                cdhRuntime,
                mockImageCatalogServer);
    }

    public String getFreeIpaImageCatalogUrlWitdDefaultImageUuid(String defaultImageUuid) {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&runtime=%s&default-image-uuid=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "freeipa-catalog",
                cdhRuntime,
                defaultImageUuid,
                mockImageCatalogServer);
    }

    public String getImageCatalogUrl() {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&runtime=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "cb-catalog",
                cdhRuntime,
                mockImageCatalogServer);
    }

    public String getPreWarmedImageCatalogUrl() {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&runtime=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "catalog-with-prewarmed",
                cdhRuntime,
                mockImageCatalogServer);
    }

    public String getPreWarmedImageCatalogUrlWithDefaultImageUuid(String defaultImageUuid) {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&runtime=%s&default-image-uuid=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "catalog-with-prewarmed",
                cdhRuntime,
                defaultImageUuid,
                mockImageCatalogServer);
    }

    public String getPreWarmedImageCatalogUrlWithCmAndCdhVersions(String cmVersion, String cdhVersion) {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&runtime=%s&cm=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "catalog-with-prewarmed",
                cdhVersion,
                cmVersion,
                mockImageCatalogServer);
    }

    public String getUpgradeImageCatalogUrl() {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&runtime=%s&mock-server-address=%s",
                mockImageCatalogServer,
                "catalog-with-for-upgrade",
                cdhUpgradeRuntime,
                mockImageCatalogServer);
    }
}

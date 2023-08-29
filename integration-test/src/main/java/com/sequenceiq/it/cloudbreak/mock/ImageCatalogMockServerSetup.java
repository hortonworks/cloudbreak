package com.sequenceiq.it.cloudbreak.mock;

import static com.sequenceiq.it.cloudbreak.CloudbreakTest.CLOUDBREAK_SERVER_ROOT;
import static com.sequenceiq.it.cloudbreak.CloudbreakTest.IMAGE_CATALOG_MOCK_SERVER_ROOT;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.util.TestParameter;

@Component
@DependsOn("cloudbreakServer")
public class ImageCatalogMockServerSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogMockServerSetup.class);

    private String cdhRuntime;

    private String cbVersion;

    private String mockImageCatalogServer;

    @Value("${integrationtest.cloudbreak.server}")
    private String defaultCloudbreakServer;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private TestParameter testParameter;

    @PostConstruct
    void initImageCatalogIfNecessary() {
        mockImageCatalogServer = testParameter.get(IMAGE_CATALOG_MOCK_SERVER_ROOT);
        cbVersion = getCloudbreakUnderTestVersion(testParameter.get(CLOUDBREAK_SERVER_ROOT));
        cdhRuntime = commonClusterManagerProperties.getRuntimeVersion();
    }

    private String getCloudbreakUnderTestVersion(String cbServerAddress) {
        WebTarget target;
        Client client = RestClientUtil.get();
        if (cbServerAddress.contains("dps.mow") || cbServerAddress.contains("cdp.mow") || cbServerAddress.contains("cdp-priv.mow")) {
            target = client.target(defaultCloudbreakServer + "/cloud/cb/info");
        } else {
            target = client.target(cbServerAddress + "/info");
        }
        Invocation.Builder request = target.request();
        try (Response response = request.get()) {
            CBVersion cbVersion = response.readEntity(CBVersion.class);
            String appVersion = cbVersion.getApp().getVersion();
            LOGGER.info("CB version: Appname: {}, version: {}", cbVersion.getApp().getName(), appVersion);
            testParameter.put("cbversion", appVersion);
            MDC.put("cbversion", appVersion);
            return appVersion;
        } catch (Exception e) {
            LOGGER.error(String.format("Cannot fetch the CB version at '%s'", cbServerAddress), e);
            throw e;
        }
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

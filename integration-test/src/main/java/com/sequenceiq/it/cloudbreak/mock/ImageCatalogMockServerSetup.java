package com.sequenceiq.it.cloudbreak.mock;

import static com.sequenceiq.it.cloudbreak.CloudbreakTest.CLOUDBREAK_SERVER_ROOT;
import static com.sequenceiq.it.cloudbreak.CloudbreakTest.IMAGE_CATALOG_MOCK_SERVER_ROOT;

import java.time.format.DateTimeFormatter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;

@Component
@DependsOn("cloudbreakServer")
public class ImageCatalogMockServerSetup {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

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
        cbVersion =  getCloudbreakUnderTestVersion(testParameter.get(CLOUDBREAK_SERVER_ROOT));
        cdhRuntime = commonClusterManagerProperties.getRuntimeVersion();
    }

    private String getCloudbreakUnderTestVersion(String cbServerAddress) {
        WebTarget target;
        Client client = RestClientUtil.get();
        if (cbServerAddress.contains("dps.mow") || cbServerAddress.contains("cdp.mow")) {
            target = client.target(defaultCloudbreakServer + "/cloud/cb/info");
        } else {
            target = client.target(cbServerAddress + "/info");
        }
        try (Response response = target.request().get()) {
            CBVersion cbVersion = response.readEntity(CBVersion.class);
            LOGGER.info("CB version: Appname: {}, version: {}", cbVersion.getApp().getName(), cbVersion.getApp().getVersion());
            return cbVersion.getApp().getVersion();
        } catch (Exception e) {
            LOGGER.error(String.format("Cannot fetch the CB version at '%s'", cbServerAddress), e);
            throw e;
        }
    }

    // DYNAMIC address http://localhost:10080/thunderhead/mock-image-catalog?catalog-name=cb-catalog&cb-version=CB-2.29.0&runtime=7.2.2
    public String getFreeIpaImageCatalogUrl() {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s",
                mockImageCatalogServer,
                "freeipa-catalog",
                cbVersion,
                cdhRuntime);
    }

    public String getFreeIpaImageCatalogUrlWitdDefaultImageUuid(String defaultImageUuid) {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s&default-image-uuid=%s",
                mockImageCatalogServer,
                "freeipa-catalog",
                cbVersion,
                cdhRuntime,
                defaultImageUuid);
    }

    public String getImageCatalogUrl() {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s",
                mockImageCatalogServer,
                "cb-catalog",
                cbVersion,
                cdhRuntime);
    }

    public String getPreWarmedImageCatalogUrl() {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s",
                mockImageCatalogServer,
                "catalog-with-prewarmed",
                cbVersion,
                cdhRuntime);
    }

    public String getPreWarmedImageCatalogUrlWithDefaultImageUuid(String defaultImageUuid) {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s&default-image-uuid=%s",
                mockImageCatalogServer,
                "catalog-with-prewarmed",
                cbVersion,
                cdhRuntime,
                defaultImageUuid);
    }

    public String getPreWarmedImageCatalogUrlWithCmAndCdhVersions(String cmVersion, String cdhVersion) {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s&cm=%s",
                mockImageCatalogServer,
                "catalog-with-prewarmed",
                cbVersion,
                cdhVersion,
                cmVersion);
    }

    public String getUpgradeImageCatalogUrl() {
        return String.format("https://%s/mock-image-catalog?catalog-name=%s&cb-version=%s&runtime=%s",
                mockImageCatalogServer,
                "catalog-with-for-upgrade",
                cbVersion,
                cdhRuntime);
    }
}

package com.sequenceiq.it.cloudbreak.mock;

import static com.sequenceiq.it.cloudbreak.CloudbreakTest.CLOUDBREAK_SERVER_ROOT;
import static com.sequenceiq.it.cloudbreak.mock.ITResponse.FREEIPA_IMAGE_CATALOG;
import static com.sequenceiq.it.cloudbreak.mock.ITResponse.IMAGE_CATALOG;
import static com.sequenceiq.it.cloudbreak.mock.ITResponse.IMAGE_CATALOG_PREWARMED;
import static com.sequenceiq.it.cloudbreak.mock.ITResponse.IMAGE_CATALOG_UPGRADE;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.spark.SparkServer;

public class ImageCatalogMockServerSetup {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogMockServerSetup.class);

    private SparkServer sparkServer;

    public ImageCatalogMockServerSetup(SparkServer sparkServer) {
        this.sparkServer = sparkServer;
    }

    public static String responseFromJsonFile(String path) {
        try (InputStream inputStream = ITResponse.class.getResourceAsStream("/mockresponse/" + path)) {
            return IOUtils.toString(inputStream);
        } catch (IOException e) {
            return "";
        }
    }

    private void startMockImageCatalogs(TestParameter testParameter) {
        String jsonPreparedICResponse = patchCbVersion(responseFromJsonFile("imagecatalog/catalog.json"), testParameter);
        String jsonFreeIPACatalogResponse = responseFromJsonFile("imagecatalog/freeipa.json");
        String jsonPreparedPrewarmICResponse = patchCbVersion(responseFromJsonFile("imagecatalog/catalog-with-prewarmed.json"), testParameter);
        String upgradeImageCatalog = patchCbVersion(responseFromJsonFile("imagecatalog/catalog-with-for-upgrade.json"), testParameter);

        startImageCatalog(IMAGE_CATALOG, jsonPreparedICResponse);
        startImageCatalog(FREEIPA_IMAGE_CATALOG, jsonFreeIPACatalogResponse);
        startImageCatalog(IMAGE_CATALOG_PREWARMED, jsonPreparedPrewarmICResponse);
        startImageCatalog(IMAGE_CATALOG_UPGRADE, upgradeImageCatalog);
    }

    private String getCloudbreakUnderTestVersion(String cbServerAddress) {
        Client client = RestClientUtil.get();
        WebTarget target = client.target(cbServerAddress + "/info");
        try (Response response = target.request().get()) {
            CBVersion cbVersion = response.readEntity(CBVersion.class);
            LOGGER.info("CB version: Appname: {}, version: {}", cbVersion.getApp().getName(), cbVersion.getApp().getVersion());
            return cbVersion.getApp().getVersion();
        } catch (Exception e) {
            LOGGER.error("Cannot fetch the CB version", e);
            throw e;
        }
    }

    public void configureImgCatalogWithExistingSparkServer(TestParameter testParameter) {
        startMockImageCatalogs(testParameter);
    }

    public void startImageCatalog(String url, String imageCatalogText) {
        sparkServer.getSparkService().get(url, (request, response) -> {
            LOGGER.info("IC get was called at: {}, {}, {}", url, request.url(), LocalDateTime.now()
                    .format(DATE_TIME_FORMATTER));
            return imageCatalogText;
        });
        sparkServer.getSparkService().head(url, (request, response) -> {
            LOGGER.info("IC head was called at: {}, {}, {}", url, request.url(), LocalDateTime.now()
                    .format(DATE_TIME_FORMATTER));
            response.header("Content-Length", String.valueOf(imageCatalogText.length()));
            return "";
        });
        sparkServer.waitEndpointToBeReady(url, imageCatalogText);
        LOGGER.info("ImageCatalog has started at: {}", sparkServer.getEndpoint() + url);
    }

    public String getFreeIpaImageCatalogUrl() {
        return String.join("", sparkServer.getEndpoint(), FREEIPA_IMAGE_CATALOG);
    }

    public String getImageCatalogUrl() {
        return String.join("", sparkServer.getEndpoint(), IMAGE_CATALOG);
    }

    public String getPreWarmedImageCatalogUrl() {
        return String.join("", sparkServer.getEndpoint(), IMAGE_CATALOG_PREWARMED);
    }

    public String getUpgradeImageCatalogUrl() {
        return String.join("", sparkServer.getEndpoint(), IMAGE_CATALOG_UPGRADE);
    }

    public SparkServer getSparkServer() {
        return sparkServer;
    }

    public String patchCbVersion(String catalogJson, TestParameter testParameter) {
        return catalogJson.replace("CB_VERSION", getCloudbreakUnderTestVersion(testParameter.get(CLOUDBREAK_SERVER_ROOT)));
    }
}

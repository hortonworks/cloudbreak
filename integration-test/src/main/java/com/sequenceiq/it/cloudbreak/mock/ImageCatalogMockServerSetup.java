package com.sequenceiq.it.cloudbreak.mock;

import static com.sequenceiq.it.cloudbreak.CloudbreakTest.CLOUDBREAK_SERVER_ROOT;
import static com.sequenceiq.it.cloudbreak.mock.ITResponse.FREEIPA_IMAGE_CATALOG;
import static com.sequenceiq.it.cloudbreak.mock.ITResponse.IMAGE_CATALOG;
import static com.sequenceiq.it.cloudbreak.mock.ITResponse.IMAGE_CATALOG_PREWARMED;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.spark.SparkServer;
import com.sequenceiq.it.cloudbreak.spark.SparkServerFactory;

@Prototype
public class ImageCatalogMockServerSetup implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogMockServerSetup.class);

    @Inject
    private SparkServerFactory sparkServerFactory;

    @Inject
    private TestParameter testParameter;

    private SparkServer sparkServer;

    public void configureImgCatalogMock() {
        sparkServer = sparkServerFactory.construct();
        startImageCatalog();
        startFreeIpaImageCatalog();
    }

    public void startImageCatalog() {
        String jsonCatalogResponse = responseFromJsonFile("imagecatalog/catalog.json");
        sparkServer.getSparkService().get(IMAGE_CATALOG, (request, response) -> patchCbVersion(jsonCatalogResponse, testParameter));
        sparkServer.getSparkService().head(IMAGE_CATALOG, (request, response) -> {
            response.header("Content-Length", String.valueOf(patchCbVersion(jsonCatalogResponse, testParameter).length()));
            return "";
        });
        LOGGER.info("ImageCatalog has started at: {}", sparkServer.getEndpoint() + IMAGE_CATALOG);
    }

    public void startFreeIpaImageCatalog() {
        String jsonCatalogResponse = responseFromJsonFile("imagecatalog/freeipa.json");
        sparkServer.getSparkService().get(FREEIPA_IMAGE_CATALOG, (request, response) -> jsonCatalogResponse);
        sparkServer.getSparkService().head(FREEIPA_IMAGE_CATALOG, (request, response) -> {
            response.header("Content-Length", String.valueOf(jsonCatalogResponse.length()));
            return "";
        });
        LOGGER.info("FreeIPA ImageCatalog has started at: {}", sparkServer.getEndpoint() + FREEIPA_IMAGE_CATALOG);
    }

    public String getFreeIpaImageCatalogUrl() {
        return String.join("", sparkServer.getEndpoint(), FREEIPA_IMAGE_CATALOG);
    }

    public String getImageCatalogUrl() {
        return String.join("", sparkServer.getEndpoint(), IMAGE_CATALOG);
    }

    public String getPreWarmedImageCatalogUrl() {
        String jsonCatalogResponse = responseFromJsonFile("imagecatalog/catalog-with-prewarmed.json");
        sparkServer.getSparkService().get(IMAGE_CATALOG_PREWARMED, (request, response) -> patchCbVersion(jsonCatalogResponse, testParameter));
        sparkServer.getSparkService().head(IMAGE_CATALOG_PREWARMED, (request, response) -> {
            response.header("Content-Length", String.valueOf(patchCbVersion(jsonCatalogResponse, testParameter).length()));
            return "";
        });
        LOGGER.info("Prewarmed ImageCatalog has started at: {}", sparkServer.getEndpoint() + IMAGE_CATALOG_PREWARMED);
        return String.join("", sparkServer.getEndpoint(), IMAGE_CATALOG_PREWARMED);
    }

    public String patchCbVersion(String catalogJson, TestParameter testParameter) {
        return catalogJson.replace("CB_VERSION", getCloudbreakUnderTestVersion(testParameter.get(CLOUDBREAK_SERVER_ROOT)));
    }

    public static String responseFromJsonFile(String path) {
        try (InputStream inputStream = ITResponse.class.getResourceAsStream("/mockresponse/" + path)) {
            return IOUtils.toString(inputStream);
        } catch (IOException e) {
            return "";
        }
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

    @Override
    public void close() {
        sparkServerFactory.release(sparkServer);
        LOGGER.info("ImageCatalog has stopped");
    }
}

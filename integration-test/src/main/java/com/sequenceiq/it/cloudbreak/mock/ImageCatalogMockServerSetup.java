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

    public void configureImgCatalogMock() throws InterruptedException {
        sparkServer = sparkServerFactory.construct();
        startMockImageCatalogs();
    }

    public void configureImgCatalogWithExistingSparkServer(SparkServer sparkServer) {
        this.sparkServer = sparkServer;
        startMockImageCatalogs();
    }

    private void startMockImageCatalogs() {
        String jsonPreparedICResponse = patchCbVersion(responseFromJsonFile("imagecatalog/catalog.json"), testParameter);
        String jsonFreeIPACatalogResponse = responseFromJsonFile("imagecatalog/freeipa.json");
        String jsonPreparedPrewarmICResponse = patchCbVersion(responseFromJsonFile("imagecatalog/catalog-with-prewarmed.json"), testParameter);

        startImageCatalog(IMAGE_CATALOG, jsonPreparedICResponse);
        startImageCatalog(FREEIPA_IMAGE_CATALOG, jsonFreeIPACatalogResponse);
        startImageCatalog(IMAGE_CATALOG_PREWARMED, jsonPreparedPrewarmICResponse);
    }

    public void startImageCatalog(String url, String imageCatalogText) {
        sparkServer.getSparkService().get(url, (request, response) -> imageCatalogText);
        sparkServer.getSparkService().head(url, (request, response) -> {
            LOGGER.info("IC head was called at: {}, {}", url, request.url());
            response.header("Content-Length", String.valueOf(imageCatalogText.length()));
            return "";
        });
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

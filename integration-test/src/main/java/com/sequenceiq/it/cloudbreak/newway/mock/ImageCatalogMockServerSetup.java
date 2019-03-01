package com.sequenceiq.it.cloudbreak.newway.mock;

import static com.sequenceiq.it.cloudbreak.newway.Mock.CLOUDBREAK_SERVER_ROOT;
import static com.sequenceiq.it.spark.ITResponse.IMAGE_CATALOG;
import static com.sequenceiq.it.spark.ITResponse.IMAGE_CATALOG_PREWARMED;

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
import com.sequenceiq.it.cloudbreak.mock.json.CBVersion;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.spark.SparkServer;
import com.sequenceiq.it.cloudbreak.newway.spark.SparkServerFactory;
import com.sequenceiq.it.spark.ITResponse;

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

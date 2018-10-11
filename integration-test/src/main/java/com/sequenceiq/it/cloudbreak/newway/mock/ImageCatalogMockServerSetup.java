package com.sequenceiq.it.cloudbreak.newway.mock;

import static com.sequenceiq.it.cloudbreak.newway.Mock.CLOUDBREAK_SERVER_ROOT;
import static com.sequenceiq.it.spark.ITResponse.IMAGE_CATALOG;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.cloudbreak.mock.json.CBVersion;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.config.SparkServer;
import com.sequenceiq.it.spark.ITResponse;

@Prototype
public class ImageCatalogMockServerSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogMockServerSetup.class);

    @Inject
    private SparkServer sparkServer;

    @Inject
    private TestParameter testParameter;

    public void configureImgCatalogMock() {
        int randomPort = ThreadLocalRandom.current().nextInt(9400, 9749 + 1);
        sparkServer.initSparkService(randomPort);
        startImageCatalog();
    }

    public void startImageCatalog() {
        String jsonCatalogResponse = responseFromJsonFile("imagecatalog/catalog.json");
        sparkServer.getSparkService().get(IMAGE_CATALOG, (request, response) -> patchCbVersion(jsonCatalogResponse, testParameter));
        LOGGER.info("ImageCatalog has started at: {}", sparkServer.getEndpoint() + IMAGE_CATALOG);
    }

    public String getImageCatalogUrl() {
        return String.join("", sparkServer.getEndpoint(), IMAGE_CATALOG);
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
        try {
            CBVersion cbVersion = target.request().get().readEntity(CBVersion.class);
            LOGGER.info("CB version: Appname: {}, version: {}", cbVersion.getApp().getName(), cbVersion.getApp().getVersion());
            return cbVersion.getApp().getVersion();
        } catch (Exception e) {
            LOGGER.error("Cannot fetch the CB version", e);
            throw e;
        }
    }

    public void stop() {
        sparkServer.stop();
        LOGGER.info("ImageCatalog has stopped");
    }
}

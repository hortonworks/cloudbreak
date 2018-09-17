package com.sequenceiq.it.cloudbreak.newway.mock;

import static com.sequenceiq.it.cloudbreak.newway.Mock.responseFromJsonFile;
import static com.sequenceiq.it.spark.ITResponse.IMAGE_CATALOG;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.cloudbreak.mock.json.CBVersion;

import spark.Service;

public class ImageCatalogServiceMock extends AbstractMock {
    public ImageCatalogServiceMock(Service sparkService) {
        super(sparkService);
    }

    public void mockImageCatalogResponse(String cbServerAddress) {
        getSparkService().get(IMAGE_CATALOG, (request, response) -> {
            String version = getCloudbreakUnderTestVersion(cbServerAddress);
            return responseFromJsonFile("imagecatalog/catalog.json").replace("CB_VERSION", version);
        });
    }

    private String getCloudbreakUnderTestVersion(String cbServerAddress) throws URISyntaxException {
        Client client = RestClientUtil.get();
        WebTarget target = client.target(new URI(cbServerAddress + "/info"));
        String version;

        try (javax.ws.rs.core.Response cbVersionResponse = target.request().get()) {
            CBVersion cbVersion = cbVersionResponse.readEntity(CBVersion.class);
            version = cbVersion.getApp().getVersion();
        }
        return version;
    }
}

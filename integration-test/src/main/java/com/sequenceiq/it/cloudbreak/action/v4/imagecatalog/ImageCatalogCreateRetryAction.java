package com.sequenceiq.it.cloudbreak.action.v4.imagecatalog;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.TestException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class ImageCatalogCreateRetryAction implements Action<ImageCatalogTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogCreateRetryAction.class);

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, format(" Image catalog post request with retry: %n"), testDto.getRequest());
        ImageCatalogV4Response response = null;
        Exception exc = null;
        int counter = 0;
        do {
            try {
                response =
                        client.getDefaultClient(testContext)
                                .imageCatalogV4Endpoint()
                                .create(client.getWorkspaceId(), testDto.getRequest());
            } catch (Exception e) {
                Log.when(LOGGER, "Image catalog could not created - retry");
                exc = e;
                Thread.sleep(2000);
                if (counter++ > 30) {
                    break;
                }
            }
        }
        while (response == null);

        if (response != null) {
            testDto.setResponse(response);
        } else {
            throw new TestException("Image catalog could not created 30 times ", exc);
        }

        Log.whenJson(LOGGER, format(" Image catalog created  successfully:%n"), testDto.getResponse());

        return testDto;
    }

}
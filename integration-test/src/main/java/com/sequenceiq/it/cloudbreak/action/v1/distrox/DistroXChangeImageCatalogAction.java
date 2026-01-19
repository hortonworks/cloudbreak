package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXChangeImageCatalogAction implements Action<DistroXChangeImageCatalogTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXChangeImageCatalogAction.class);

    public DistroXChangeImageCatalogTestDto action(TestContext testContext, DistroXChangeImageCatalogTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, format(" DistroX put request:%n"), testDto.getRequest());
        client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .changeImageCatalog(testContext.given(DistroXTestDto.class).getResponse().getName(), testDto.getRequest());
        Log.when(LOGGER, format(" DistroX change image catalog to: %s", testDto.getRequest().getImageCatalog()));
        return testDto;
    }
}

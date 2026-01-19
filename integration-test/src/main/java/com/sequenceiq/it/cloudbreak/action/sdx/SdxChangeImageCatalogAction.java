package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxChangeImageCatalogAction implements Action<SdxChangeImageCatalogTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxChangeImageCatalogAction.class);

    public SdxChangeImageCatalogTestDto action(TestContext testContext, SdxChangeImageCatalogTestDto testDto, SdxClient client) throws Exception {
        Log.whenJson(LOGGER, format(" SDX put request:%n"), testDto.getRequest());
        client.getDefaultClient(testContext)
                .sdxEndpoint()
                .changeImageCatalog(testContext.given(SdxInternalTestDto.class).getResponse().getName(), testDto.getRequest());
        Log.when(LOGGER, format(" SDX change image catalog to: %s", testDto.getRequest().getImageCatalog()));
        return testDto;
    }
}

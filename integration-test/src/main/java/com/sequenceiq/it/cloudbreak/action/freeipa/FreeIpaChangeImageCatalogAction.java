package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeipaChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaChangeImageCatalogAction implements Action<FreeipaChangeImageCatalogTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaChangeImageCatalogAction.class);

    public FreeipaChangeImageCatalogTestDto action(TestContext testContext, FreeipaChangeImageCatalogTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, format(" FreeIPA put request:%n"), testDto.getRequest());
        client.getDefaultClient(testContext)
                .getFreeIpaV1Endpoint()
                        .changeImageCatalog(testContext.given(EnvironmentTestDto.class).getCrn(), testDto.getRequest());
        Log.when(LOGGER, format(" FreeIPA change image catalog to: %s", testDto.getRequest().getImageCatalog()));
        return testDto;
    }
}

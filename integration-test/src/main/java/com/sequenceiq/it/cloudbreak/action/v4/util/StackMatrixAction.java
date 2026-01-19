package com.sequenceiq.it.cloudbreak.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackMatrixAction implements Action<StackMatrixTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackMatrixAction.class);

    @Override
    public StackMatrixTestDto action(TestContext testContext, StackMatrixTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        ImageCatalogTestDto imageCatalogTestDto = testContext.get(ImageCatalogTestDto.class);
        testDto.setResponse(cloudbreakClient.getDefaultClient(testContext).utilV4Endpoint().getStackMatrix(
                imageCatalogTestDto != null ? imageCatalogTestDto.getName() : null,
                testDto.getCloudPlatform().name(),
                testDto.isGovCloud(),
                testDto.getOs(),
                NullUtil.getIfNotNull(testDto.getArchitecture(), Architecture::getName)));
        Log.whenJson(LOGGER, "Obtaining stack matrix response:\n", testDto.getResponse());

        return testDto;
    }
}

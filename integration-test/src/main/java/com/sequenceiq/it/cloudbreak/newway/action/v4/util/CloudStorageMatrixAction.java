package com.sequenceiq.it.cloudbreak.newway.action.v4.util;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.util.CloudStorageMatrixTestDto;

public class CloudStorageMatrixAction implements Action<CloudStorageMatrixTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageMatrixAction.class);

    @Override
    public CloudStorageMatrixTestDto action(TestContext testContext, CloudStorageMatrixTestDto testDto, CloudbreakClient client) throws Exception {
        String logInitMessage = "Obtaining cloud storage matrix";
        LOGGER.info("{}", logInitMessage);
        testDto.setResponses(new HashSet<>(client.getCloudbreakClient().utilV4Endpoint().getCloudStorageMatrix(testDto.getStackVersion()).getResponses()));
        LOGGER.info("{} was successful", logInitMessage);
        return testDto;
    }
}

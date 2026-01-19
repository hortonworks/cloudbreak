package com.sequenceiq.it.cloudbreak.action.v4.util;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.CloudStorageMatrixTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class CloudStorageMatrixAction implements Action<CloudStorageMatrixTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageMatrixAction.class);

    @Override
    public CloudStorageMatrixTestDto action(TestContext testContext, CloudStorageMatrixTestDto testDto, CloudbreakClient client) throws Exception {
        testDto.setResponses(new HashSet<>(client.getDefaultClient(testContext).utilV4Endpoint()
                .getCloudStorageMatrix(testDto.getStackVersion()).getResponses()));
        Log.whenJson(LOGGER, "Cloud storage matrix response:\n", testDto.getResponses());
        return testDto;
    }
}

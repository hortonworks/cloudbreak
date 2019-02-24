package com.sequenceiq.it.cloudbreak.newway.action.storagematrix;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.storagematrix.CloudStorageMatrixTestDto;

public class CloudStorageMatrixTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageMatrixTestAction.class);

    private CloudStorageMatrixTestAction() {
    }

    public static CloudStorageMatrixTestDto getCloudStorageMatrix(TestContext testContext, CloudStorageMatrixTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining cloud storage matrix";
        LOGGER.info("{}", logInitMessage);
        entity.setResponses(new HashSet<>(client.getCloudbreakClient().utilV4Endpoint().getCloudStorageMatrix(entity.getStackVersion()).getResponses()));
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}

package com.sequenceiq.it.cloudbreak.assertion.util;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.util.CloudStorageMatrixTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class CloudStorageMatrixTestAssertion {

    private CloudStorageMatrixTestAssertion() {
    }

    public static Assertion<CloudStorageMatrixTestDto, CloudbreakClient> matrixIsNotEmpty() {
        return (testContext, entity, cloudbreakClient) -> {
            assertFalse(entity.getResponses().isEmpty());
            return entity;
        };
    }

}

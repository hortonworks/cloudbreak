package com.sequenceiq.it.cloudbreak.newway.assertion.util;

import static org.junit.Assert.assertFalse;

import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.dto.util.CloudStorageMatrixTestDto;

public class CloudStorageMatrixTestAssertion {

    private CloudStorageMatrixTestAssertion() {
    }

    public static AssertionV2<CloudStorageMatrixTestDto> matrixIsNotEmpty() {
        return (testContext, entity, cloudbreakClient) -> {
            assertFalse(entity.getResponses().isEmpty());
            return entity;
        };
    }

}

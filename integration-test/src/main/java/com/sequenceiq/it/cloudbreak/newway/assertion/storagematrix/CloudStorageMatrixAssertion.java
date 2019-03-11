package com.sequenceiq.it.cloudbreak.newway.assertion.storagematrix;

import static org.junit.Assert.assertFalse;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.util.CloudStorageMatrixTestDto;

public class CloudStorageMatrixAssertion {

    private CloudStorageMatrixAssertion() {
    }

    public static CloudStorageMatrixTestDto matrixIsNotEmpty(TestContext tc, CloudStorageMatrixTestDto entity, CloudbreakClient cc) {
        assertFalse(entity.getResponses().isEmpty());
        return entity;
    }

}

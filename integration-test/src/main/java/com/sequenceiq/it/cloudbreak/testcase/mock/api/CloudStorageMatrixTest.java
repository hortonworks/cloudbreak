package com.sequenceiq.it.cloudbreak.testcase.mock.api;

import javax.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.CommonAssert;
import com.sequenceiq.it.cloudbreak.assertion.util.CloudStorageMatrixTestAssertion;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.dto.util.CloudStorageMatrixTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class CloudStorageMatrixTest extends AbstractIntegrationTest {

    @Inject
    private UtilTestClient utilTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((MockedTestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a cloud storage matrix",
            when = "the cloudstorage endpoint is called",
            then = "a matrix with the supported cloud storages is returned")
    public void getCloudStorageMatrixThenReturnSupportedCloudStorages(MockedTestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();

        testContext
                .given(CloudStorageMatrixTestDto.class)
                .when(utilTestClient.cloudStorageMatrix(), RunningParameter.key(generatedKey))
                .then(CommonAssert::responsesExists, RunningParameter.key(generatedKey))
                .then(CloudStorageMatrixTestAssertion.matrixIsNotEmpty(), RunningParameter.key(generatedKey))
                .validate();
    }

}

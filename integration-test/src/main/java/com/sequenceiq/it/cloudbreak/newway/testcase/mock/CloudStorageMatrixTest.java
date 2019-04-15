package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.assertion.util.CloudStorageMatrixTestAssertion.matrixIsNotEmpty;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.assertion.CommonAssert;
import com.sequenceiq.it.cloudbreak.newway.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.util.CloudStorageMatrixTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

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
                .when(utilTestClient.cloudStorageMatrix(), key(generatedKey))
                .then(CommonAssert::responsesExists, key(generatedKey))
                .then(matrixIsNotEmpty(), key(generatedKey))
                .validate();
    }

}

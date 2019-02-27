package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.storagematrix.CloudStorageMatrixTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.CommonAssert;
import com.sequenceiq.it.cloudbreak.newway.assertion.storagematrix.CloudStorageMatrixAssertion;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.storagematrix.CloudStorageMatrixTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class CloudStorageMatrixTest extends AbstractIntegrationTest {

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
        String generatedKey = getNameGenerator().getRandomNameForResource();

        testContext
                .given(CloudStorageMatrixTestDto.class)
                .when(CloudStorageMatrixTestAction::getCloudStorageMatrix, key(generatedKey))
                .then(CommonAssert::responsesExists, key(generatedKey))
                .then(CloudStorageMatrixAssertion::matrixIsNotEmpty, key(generatedKey))
                .validate();
    }

}

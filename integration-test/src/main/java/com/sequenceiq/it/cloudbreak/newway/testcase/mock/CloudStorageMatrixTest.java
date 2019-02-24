package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.storagematrix.CloudStorageMatrixTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.CommonAssert;
import com.sequenceiq.it.cloudbreak.newway.assertion.storagematrix.CloudStorageMatrixAssertion;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.storagematrix.CloudStorageMatrixTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class CloudStorageMatrixTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((MockedTestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetCloudStorageMatrix(MockedTestContext testContext) {
        testContext
                .given(CloudStorageMatrixTestDto.class)
                .when(CloudStorageMatrixTestAction::getCloudStorageMatrix)
                .then(CommonAssert::responsesExists)
                .then(CloudStorageMatrixAssertion::matrixIsNotEmpty)
                .validate();
    }

}

package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.repoconfig.RepoConfigValidationTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.repoconfig.RepoConfigValidationTestData;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.repoconfig.RepoConfigValidationTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class RepoConfigValidationTest extends AbstractIntegrationTest {

    private static final String DATA_PROVIDER_FOR_REPO_CONFIG_TEST = "contextAndTestData";

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = DATA_PROVIDER_FOR_REPO_CONFIG_TEST)
    public void testPostRepositoryConfigValidationAgainstDifferentlyParameterizedRequest(MockedTestContext testContext, RepoConfigValidationTestData testData) {
        testContext
                .given(RepoConfigValidationTestDto.class)
                .withRequest(testData.request())
                .when(RepoConfigValidationTestAction::postRepositoryConfigValidation)
                .then(testData::resultValidation)
                .validate();
    }

    @DataProvider(name = DATA_PROVIDER_FOR_REPO_CONFIG_TEST)
    public Object[][] dataProvider() {
        var testDataValues = RepoConfigValidationTestData.values();
        var data = new Object[testDataValues.length][2];
        var testContext = getBean(MockedTestContext.class);
        for (int i = 0; i < testDataValues.length; i++) {
            data[i][0] = testContext;
            data[i][1] = testDataValues[i];
        }
        return data;
    }

}

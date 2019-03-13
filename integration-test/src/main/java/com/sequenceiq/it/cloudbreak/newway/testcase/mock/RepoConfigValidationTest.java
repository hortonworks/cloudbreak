package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.repoconfig.RepoConfigValidationTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.repoconfig.RepoConfigValidationTestData;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
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
    public void testPostRepositoryConfigValidationAgainstDifferentlyParameterizedRequest(
            MockedTestContext testContext,
            RepoConfigValidationTestData testData,
            @Description TestCaseDescription testCaseDescription) {
        String generatedKey = getNameGenerator().getRandomNameForResource();

        testContext
                .given(generatedKey, RepoConfigValidationTestDto.class)
                .withRequest(testData.request())
                .when(RepoConfigValidationTestAction::postRepositoryConfigValidation, key(generatedKey))
                .then(testData::resultValidation, key(generatedKey))
                .validate();
    }

    @DataProvider(name = DATA_PROVIDER_FOR_REPO_CONFIG_TEST)
    public Object[][] dataProvider() {
        var testDataValues = RepoConfigValidationTestData.values();
        var data = new Object[testDataValues.length][3];
        var testContext = getBean(MockedTestContext.class);
        for (int i = 0; i < testDataValues.length; i++) {
            data[i][0] = testContext;
            data[i][1] = testDataValues[i];
            data[i][2] =
                    new TestCaseDescription.TestCaseDescriptionBuilder()
                            .given("a repository config with " + testDataValues[i])
                            .when("calling validation endpoint")
                            .then("the repository should be valid");
        }
        return data;
    }

}

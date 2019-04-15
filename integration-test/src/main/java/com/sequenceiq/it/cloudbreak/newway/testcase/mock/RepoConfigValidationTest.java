package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.assertion.util.RepoConfigValidationTestAssertion;
import com.sequenceiq.it.cloudbreak.newway.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.util.RepoConfigValidationTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class RepoConfigValidationTest extends AbstractIntegrationTest {

    private static final String DATA_PROVIDER_FOR_REPO_CONFIG_TEST = "contextAndTestData";

    @Inject
    private UtilTestClient utilTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
    }

    @Test(dataProvider = DATA_PROVIDER_FOR_REPO_CONFIG_TEST)
    public void testPostRepositoryConfigValidationAgainstDifferentlyParameterizedRequest(
            MockedTestContext testContext,
            RepoConfigValidationTestAssertion testData,
            @Description TestCaseDescription testCaseDescription) {

        String generatedKey = resourcePropertyProvider().getName();
        testContext
                .given(generatedKey, RepoConfigValidationTestDto.class)
                .withRequest(testData.request())
                .when(utilTestClient.repoConfigValidationV4(), key(generatedKey))
                .then(testData::resultValidation, key(generatedKey))
                .validate();
    }

    @DataProvider(name = DATA_PROVIDER_FOR_REPO_CONFIG_TEST)
    public Object[][] dataProvider() {
        var testDataValues = RepoConfigValidationTestAssertion.values();
        var data = new Object[testDataValues.length][3];
        for (int i = 0; i < testDataValues.length; i++) {
            var testContext = getBean(MockedTestContext.class);
            createDefaultUser(testContext);
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

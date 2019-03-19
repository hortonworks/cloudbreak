package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.assertion.util.SecurityRulesTestAssertion.coreIsNotEmpty;
import static com.sequenceiq.it.cloudbreak.newway.assertion.util.SecurityRulesTestAssertion.gatewayIsNotEmpty;

import javax.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.assertion.CommonAssert;
import com.sequenceiq.it.cloudbreak.newway.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription.TestCaseDescriptionBuilder;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.util.SecurityRulesTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class SecurityRulesTest extends AbstractIntegrationTest {

    private static final String DATA_PROVIDER_FOR_SECURITY_RULES_TEST = "contextAndBoolean";

    @Inject
    private UtilTestClient utilTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = DATA_PROVIDER_FOR_SECURITY_RULES_TEST)
    public void getSecurityRulesWithDifferentConfigurations(
            MockedTestContext testContext,
            Boolean knoxEnabled,
            @Description TestCaseDescription testCaseDescription) {
        testContext
                .given(SecurityRulesTestDto.class)
                .withKnoxEnabled(knoxEnabled)
                .when(utilTestClient.securityRulesV4())
                .then(CommonAssert::responseExists)
                .then(coreIsNotEmpty())
                .then(gatewayIsNotEmpty())
                .validate();
    }

    @DataProvider(name = DATA_PROVIDER_FOR_SECURITY_RULES_TEST)
    public Object[][] dataProvider() {
        var testContext = getBean(MockedTestContext.class);
        return new Object[][]{
                {
                        testContext,
                        false,
                        new TestCaseDescriptionBuilder()
                                .given("Query default security rules for cluster which is NOT knox enabled")
                                .when("calling get default security groups")
                                .then("returns default security rules and knox port")
                },
                {
                        testContext,
                        true,
                        new TestCaseDescriptionBuilder()
                                .given("Query default security rules for cluster which is knox enabled")
                                .when("calling get default security groups")
                                .then("returns default security rules without knox port")
                },
                {
                        testContext,
                        null,
                        new TestCaseDescriptionBuilder()
                                .given("Query default security rules for cluster with null")
                                .when("calling get default security groups")
                                .then("returns default security rules without knox port")
                }
        };
    }

}

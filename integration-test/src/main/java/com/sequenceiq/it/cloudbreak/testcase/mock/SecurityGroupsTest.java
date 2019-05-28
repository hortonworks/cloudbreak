package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.ConnectorTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformSecurityGroupsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class SecurityGroupsTest extends AbstractIntegrationTest {

    @Inject
    private ConnectorTestClient connectorTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void testGetSecurityGroupsByCredentialNameWhenCredentialIsInvalid(
            MockedTestContext testContext, String credentialName, String exceptionKey,
            Class<Exception> exception, String msg, @Description TestCaseDescription testCaseDescription) {
        testContext
                .given(PlatformSecurityGroupsTestDto.class)
                .withCredentialName(credentialName)
                .when(connectorTestClient.securityGroups(), RunningParameter.key(exceptionKey))
                .expect(exception, RunningParameter.key(exceptionKey).withExpectedMessage(msg))
                .validate();
    }

    @DataProvider(name = "contextWithCredentialNameAndException")
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {getBean(MockedTestContext.class), "", "badRequest", BadRequestException.class,
                        "The credentialId or the credentialName must be specified in the request",
                        TestCaseDescription.TestCaseDescriptionBuilder.createWithGiven("platform security groups").when("credenetial name is null")
                                .then("credential validator error")},
                {getBean(MockedTestContext.class), null, "badRequest", BadRequestException.class,
                        "The credentialId or the credentialName must be specified in the request",
                        TestCaseDescription.TestCaseDescriptionBuilder.createWithGiven("platform security groups").when("credenetial name is null")
                                .then("credential validator error")},
                {getBean(MockedTestContext.class), "andNowForSomethingCompletelyDifferent", "notfound", NotFoundException.class,
                        "No credential found with name 'andNowForSomethingCompletelyDifferent'",
                        TestCaseDescription.TestCaseDescriptionBuilder.createWithGiven("platform security groups").when("credenetial name is null")
                                .then("credential not found error")}
        };
    }

}

package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription.TestCaseDescriptionBuilder.createWithGiven;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.action.securitygroup.PlatformSecurityGroupsTestAction;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.securitygroup.PlatformSecurityGroupsTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class SecurityGroupsTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "platform security groups", when = "filter by a valid ceredential name", then = "get the list of security groups")
    public void testGetSecurityGroupsByCredentialName(MockedTestContext testContext) {
        String credentialName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(CredentialTestClient::create)
                .given(PlatformSecurityGroupsTestDto.class)
                .withCredentialName(credentialName)
                .when(PlatformSecurityGroupsTestAction::getSecurityGroups);
    }

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void testGetSecurityGroupsByCredentialNameWhenCredentialIsInvalid(MockedTestContext testContext, String credentialName, String exceptionKey,
            Class<Exception> exception, String msg, @Description TestCaseDescription testCaseDescription) {
        testContext
                .given(PlatformSecurityGroupsTestDto.class)
                .withCredentialName(credentialName)
                .when(PlatformSecurityGroupsTestAction::getSecurityGroups, key(exceptionKey))
                .expect(exception, key(exceptionKey).withExpectedMessage(msg))
                .validate();
    }

    @DataProvider(name = "contextWithCredentialNameAndException")
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {getBean(MockedTestContext.class), "", "badRequest", BadRequestException.class,
                        "The credentialId or the credentialName must be specified in the request",
                        createWithGiven("platform security groups").when("credenetial name is null").then("credential validator error")},
                {getBean(MockedTestContext.class), null, "badRequest", BadRequestException.class,
                        "The credentialId or the credentialName must be specified in the request",
                        createWithGiven("platform security groups").when("credenetial name is null").then("credential validator error")},
                {getBean(MockedTestContext.class), "andNowForSomethingCompletelyDifferent", "forbidden", ForbiddenException.class,
                        "No credential found with name 'andNowForSomethingCompletelyDifferent'",
                        createWithGiven("platform security groups").when("credenetial name is null").then("credential not found error")}
        };
    }

}

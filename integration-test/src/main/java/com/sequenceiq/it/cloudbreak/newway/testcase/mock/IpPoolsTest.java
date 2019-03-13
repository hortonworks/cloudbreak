package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.ip.PlatformIpPoolsTestAction;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.ip.PlatformIpPoolsTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class IpPoolsTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a MOCK credential",
            when = "calling get ip pools from the provider",
            then = "getting back the ip pool list")
    public void testGetIpPoolsByCredentialName(MockedTestContext testContext) {
        String credentialName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(CredentialTestClient::create)
                .given(PlatformIpPoolsTestDto.class)
                .withCredentialName(credentialName)
                .when(PlatformIpPoolsTestAction::getIpPools);
    }

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void testGetIpPoolsByCredentialNameWhenCredentialIsInvalid(MockedTestContext testContext, String credentialName,
            Class<Exception> exception, @Description TestCaseDescription testCaseDescription) {
        String exceptionKey = getNameGenerator().getRandomNameForResource();

        testContext
                .given(PlatformIpPoolsTestDto.class)
                .withCredentialName(credentialName)
                .when(PlatformIpPoolsTestAction::getIpPools, key(exceptionKey))
                .expect(exception, key(exceptionKey))
                .validate();
    }

    @DataProvider(name = "contextWithCredentialNameAndException")
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {
                        getBean(MockedTestContext.class),
                        "",
                        BadRequestException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("created a MOCK credential")
                                .when("calling ip pools endpoint with an empty credential name")
                                .then("getting a BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        null,
                        BadRequestException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("created a MOCK credential")
                                .when("calling ip pools endpoint with a 'null' as credential name")
                                .then("getting a BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        "andNowForSomethingCompletelyDifferent",
                        ForbiddenException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("created a MOCK credential")
                                .when("calling ip pools endpoint with a credential which is not in the same account")
                                .then("getting a ForbiddenException")
                }
        };
    }

}

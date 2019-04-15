package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.client.ConnectorTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformIpPoolsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class IpPoolsTest extends AbstractIntegrationTest {

    @Inject
    private ConnectorTestClient connectorTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a MOCK credential",
            when = "calling get ip pools from the provider",
            then = "getting back the ip pool list")
    public void testGetIpPoolsByCredentialName(MockedTestContext testContext) {
        String credentialName = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(credentialTestClient.createV4())
                .given(PlatformIpPoolsTestDto.class)
                .withCredentialName(credentialName)
                .when(connectorTestClient.ipPools())
                .validate();
    }

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void testGetIpPoolsByCredentialNameWhenCredentialIsInvalid(MockedTestContext testContext, String credentialName,
            Class<Exception> exception, @Description TestCaseDescription testCaseDescription) {
        String exceptionKey = resourcePropertyProvider().getName();

        testContext
                .given(PlatformIpPoolsTestDto.class)
                .withCredentialName(credentialName)
                .when(connectorTestClient.ipPools(), key(exceptionKey))
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

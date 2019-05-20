package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.ConnectorTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformGatewaysTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class GatewaysTest extends AbstractIntegrationTest {

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
            given = "created a MOCK credential",
            when = "calling gateways endpoint with that credential for getting cloud gateways",
            then = "getting a list with mock gateways")
    public void testGetPlatformGatewaysByCredentialName(MockedTestContext testContext) {
        String credentialName = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(credentialTestClient.createV4())
                .given(PlatformGatewaysTestDto.class)
                .withCredentialName(credentialName)
                .when(connectorTestClient.gateways())
                .validate();
    }

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void testGetPlatformGatewaysByCredentialNameWhenCredentialIsInvalid(MockedTestContext testContext, String credentialName,
            Class<Exception> exception, @Description TestCaseDescription testCaseDescription) {
        String exceptionKey = resourcePropertyProvider().getName();
        testContext
                .given(PlatformGatewaysTestDto.class)
                .withCredentialName(credentialName)
                .when(connectorTestClient.gateways(), RunningParameter.key(exceptionKey))
                .expect(exception, RunningParameter.key(exceptionKey))
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
                                .when("calling gateways endpoint with a non existing credential for getting cloud gateways")
                                .then("getting a BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        null,
                        BadRequestException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("Testing database with unsupported database type")
                                .when("calling gateways endpoint with a 'null' credential for getting cloud gateways")
                                .then("getting a BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        "andNowForSomethingCompletelyDifferent",
                        ForbiddenException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("Testing database with unsupported database type")
                                .when("calling gateways endpoint with a credential which not related to that account for getting cloud gateways")
                                .then("getting a  ForbiddenException")
                }
        };
    }

}

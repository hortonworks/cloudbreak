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
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformNetworksTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class NetworksTest extends AbstractIntegrationTest {

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
            given = "a valid MOCK credential",
            when = "list networks",
            then = "getting back a network list")
    public void testGetPlatformNetworksByCredentialName(MockedTestContext testContext) {
        String credentialName = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(credentialTestClient.createV4())
                .given(PlatformNetworksTestDto.class)
                .withCredentialName(credentialName)
                .when(connectorTestClient.networks())
                .validate();
    }

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void testGetPlatformNetworksByCredentialNameWhenCredentialIsInvalid(
            MockedTestContext testContext,
            String credentialName,
            Class<Exception> exception,
            @Description TestCaseDescription testCaseDescription) {
        String exceptionKey = resourcePropertyProvider().getName();

        testContext
                .given(PlatformNetworksTestDto.class)
                .withCredentialName(credentialName)
                .when(connectorTestClient.networks(), key(exceptionKey))
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
                                .given("an empty credentialname")
                                .when("calling get networks on provider side")
                                .then("getting BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        null,
                        BadRequestException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("an 'null' credentialname")
                                .when("calling get networks on provider side")
                                .then("getting BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        "andNowForSomethingCompletelyDifferent",
                        ForbiddenException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a credentialname which not relates to the workspace")
                                .when("calling get networks on provider side")
                                .then("getting ForbiddenException")
                }
        };
    }

}

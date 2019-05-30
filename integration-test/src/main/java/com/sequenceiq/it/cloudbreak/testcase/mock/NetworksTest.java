package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.ConnectorTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformNetworksTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class NetworksTest extends AbstractIntegrationTest {

    @Inject
    private ConnectorTestClient connectorTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
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
                .when(connectorTestClient.networks(), RunningParameter.key(exceptionKey))
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
                        InternalServerErrorException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a credentialname which not relates to the workspace")
                                .when("calling get networks on provider side")
                                .then("getting NotFoundException")
                }
        };
    }

}

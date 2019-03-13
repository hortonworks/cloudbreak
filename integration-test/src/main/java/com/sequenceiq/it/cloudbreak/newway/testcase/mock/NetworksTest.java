package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.network.PlatformNetworksTestAction;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.network.PlatformNetworksTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class NetworksTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid MOCK credential",
            when = "list networks",
            then = "getting back a network list")
    public void testGetPlatformNetworksByCredentialName(MockedTestContext testContext) {
        String credentialName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(CredentialTestClient::create)
                .given(PlatformNetworksTestDto.class)
                .withCredentialName(credentialName)
                .when(PlatformNetworksTestAction::getPlatformNetworks);
    }

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void testGetPlatformNetworksByCredentialNameWhenCredentialIsInvalid(
            MockedTestContext testContext,
            String credentialName,
            Class<Exception> exception,
            @Description TestCaseDescription testCaseDescription) {
        String exceptionKey = getNameGenerator().getRandomNameForResource();

        testContext
                .given(PlatformNetworksTestDto.class)
                .withCredentialName(credentialName)
                .when(PlatformNetworksTestAction::getPlatformNetworks, key(exceptionKey))
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

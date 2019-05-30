package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.ConnectorTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformIpPoolsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class IpPoolsTest extends AbstractIntegrationTest {

    @Inject
    private ConnectorTestClient connectorTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
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
                        InternalServerErrorException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("created a MOCK credential")
                                .when("calling ip pools endpoint with a credential which is not in the same account")
                                .then("getting a NotFoundException")
                }
        };
    }

}

package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.ConnectorTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformRegionTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class RegionTest extends AbstractIntegrationTest {

    @Inject
    private ConnectorTestClient connectorTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void testGetRegionsByCredentialNameWhenCredentialIsInvalid(
            MockedTestContext testContext,
            String credentialName,
            Class<Exception> exception,
            @Description TestCaseDescription testCaseDescription) {
        String exceptionKey = resourcePropertyProvider().getName();
        testContext
                .given(PlatformRegionTestDto.class)
                .withCredentialName(credentialName)
                .when(connectorTestClient.regions(), RunningParameter.key(exceptionKey))
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
                                .given("a MOCK credential name which is empty")
                                .when("calling get region on provider side")
                                .then("getting BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        null,
                        BadRequestException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a MOCK credential name which is 'null'")
                                .when("calling get region on provider side")
                                .then("getting BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        "andNowForSomethingCompletelyDifferent",
                        NotFoundException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a MOCK credential name which does not belongs to that workspace")
                                .when("calling get region on provider side")
                                .then("getting NotFoundException")
                }
        };
    }

}

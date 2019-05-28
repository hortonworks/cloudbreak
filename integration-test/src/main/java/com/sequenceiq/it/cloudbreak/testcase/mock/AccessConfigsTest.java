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
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformAccessConfigsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class AccessConfigsTest extends AbstractIntegrationTest {

    @Inject
    private ConnectorTestClient connectorTestClient;

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void testGetAccessConfigsByCredentialNameWhenCredentialIsInvalid(
            MockedTestContext testContext,
            String credentialName,
            Class<Exception> exception,
            @Description TestCaseDescription description) {
        String generatedKey = resourcePropertyProvider().getName();
        testContext
                .given(PlatformAccessConfigsTestDto.class)
                .withCredentialName(credentialName)
                .when(connectorTestClient.accessConfigs(), RunningParameter.key(generatedKey))
                .expect(exception, RunningParameter.key(generatedKey))
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
                                .given("a working Cloudbreak")
                                .when("get access config is called with empty credential name")
                                .then("returns with BadRequestException for MOCK")
                },
                {
                        getBean(MockedTestContext.class),
                        null,
                        BadRequestException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a working Cloudbreak")
                                .when("get access config is called with null credential name")
                                .then("returns with BadRequestException for MOCK")
                },
                {
                        getBean(MockedTestContext.class),
                        "andNowForSomethingCompletelyDifferent",
                        NotFoundException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a working Cloudbreak")
                                .when("get access config is called with not existing credential name")
                                .then("returns with NotFoundException for MOCK")}
        };
    }

}

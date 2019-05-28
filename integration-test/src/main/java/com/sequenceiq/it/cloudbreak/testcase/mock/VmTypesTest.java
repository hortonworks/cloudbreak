package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.testng.annotations.BeforeMethod;
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

public class VmTypesTest extends AbstractIntegrationTest {

    @Inject
    private ConnectorTestClient connectorTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void testGetPlatformVmtypesByCredentialNameWhenCredentialIsInvalid(
            MockedTestContext testContext,
            String credentialName,
            Class<Exception> exception,
            @Description TestCaseDescription description) {
        String generatedKey = resourcePropertyProvider().getName();

        testContext
                .given(PlatformRegionTestDto.class)
                .withCredentialName(credentialName)
                .when(connectorTestClient.regions(), RunningParameter.key(generatedKey))
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
                                .given("a region")
                                .when("without credential name")
                                .then("throw bad request exception")},
                {
                        getBean(MockedTestContext.class),
                        null,
                        BadRequestException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a region")
                                .when("credential name is empty")
                                .then("throw bad request exception")},
                {
                        getBean(MockedTestContext.class),
                        "andNowForSomethingCompletelyDifferent",
                        NotFoundException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a region")
                                .when("credential name is null")
                                .then("throw NotFoundException")}
        };
    }

}

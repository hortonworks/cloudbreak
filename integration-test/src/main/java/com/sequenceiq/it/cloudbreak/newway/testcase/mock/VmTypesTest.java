package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.client.ConnectorTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription.TestCaseDescriptionBuilder;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformRegionTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformVmTypesTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class VmTypesTest extends AbstractIntegrationTest {

    @Inject
    private ConnectorTestClient connectorTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a credential with random name",
            when = "retrive platform VMs by the credential name",
            then = "list of platform VMs")
    public void testGetPlatformVmtypesByCredentialName(MockedTestContext testContext) {
        String credentialName = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(credentialTestClient.createV4())
                .given(PlatformVmTypesTestDto.class)
                .withCredentialName(credentialName)
                .when(connectorTestClient.vmTypes())
                .validate();
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
                .when(connectorTestClient.regions(), key(generatedKey))
                .expect(exception, key(generatedKey))
                .validate();
    }

    @DataProvider(name = "contextWithCredentialNameAndException")
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {
                        getBean(MockedTestContext.class),
                        "",
                        BadRequestException.class,
                        new TestCaseDescriptionBuilder()
                                .given("a region")
                                .when("without credential name")
                                .then("throw bad request exception")},
                {
                        getBean(MockedTestContext.class),
                        null,
                        BadRequestException.class,
                        new TestCaseDescriptionBuilder()
                                .given("a region")
                                .when("credential name is empty")
                                .then("throw bad request exception")},
                {
                        getBean(MockedTestContext.class),
                        "andNowForSomethingCompletelyDifferent",
                        ForbiddenException.class,
                        new TestCaseDescriptionBuilder()
                                .given("a region")
                                .when("credential name is null")
                                .then("throw bad request exception")}
        };
    }

}

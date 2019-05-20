package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.ConnectorTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformSshKeysTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class SSHKeysTest extends AbstractIntegrationTest {

    @Inject
    private ConnectorTestClient connectorTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "a valid MOCK credential",
            when = "get ssh keys",
            then = "getting back the MOCK related ssh keys")
    public void testGetSSHKeysByCredentialName(MockedTestContext testContext) {
        String credentialName = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(credentialTestClient.createV4())
                .given(PlatformSshKeysTestDto.class)
                .withCredentialName(credentialName)
                .when(connectorTestClient.sshKeys())
                .validate();
    }

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void testGetSSHKeysByCredentialNameWhenCredentialIsInvalid(
            MockedTestContext testContext,
            String credentialName,
            String exceptionKey,
            Class<Exception> exception,
            @Description TestCaseDescription testCaseDescription) {
        testContext
                .given(PlatformSshKeysTestDto.class)
                .withCredentialName(credentialName)
                .when(connectorTestClient.sshKeys(), key(exceptionKey))
                .expect(exception, key(exceptionKey))
                .validate();
    }

    @DataProvider(name = "contextWithCredentialNameAndException")
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {getBean(MockedTestContext.class), "", "badRequest", BadRequestException.class,
                        TestCaseDescription.TestCaseDescriptionBuilder.createWithGiven("platform ssh keys")
                                .when("credential name is empty")
                                .then("get bad request ecxeption")},
                {getBean(MockedTestContext.class), null, "badRequest", BadRequestException.class,
                        TestCaseDescription.TestCaseDescriptionBuilder.createWithGiven("platform ssh keys")
                                .when("credential name is null")
                                .then("get bad request ecxeption")},
                {getBean(MockedTestContext.class), "andNowForSomethingCompletelyDifferent", "forbidden",
                        ForbiddenException.class, TestCaseDescription.TestCaseDescriptionBuilder.createWithGiven("platform ssh keys")
                        .when("credential name is not exists")
                        .then("get forbidden ecxeption")}
        };
    }
}

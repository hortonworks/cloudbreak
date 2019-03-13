package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.encryptionkeys.PlatformEncryptionKeysTestAction;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.encryption.PlatformEncryptionKeysTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class EncryptionKeysTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a prepared MOCK credential with encryption keys",
            when = "calling get encryption keys endpoint",
            then = "the MOCK related encryption keys should be returned")
    public void getPlatformEncryptionKeysWithMockCredentialThenReturnWithPlatformRelatedKeys(MockedTestContext testContext) {
        String credentialName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(CredentialTestClient::create, key(credentialName))
                .given(PlatformEncryptionKeysTestDto.class)
                .withCredentialName(credentialName)
                .when(PlatformEncryptionKeysTestAction::getEncryptionKeys, key(credentialName))
                .validate();
    }

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void getPlatformEncryptionKeysWithMockCredentialThenReturnWithPlatformRelatedKeys(
            MockedTestContext testContext,
            String credentialName,
            Class<Exception> exception,
            @Description TestCaseDescription testCaseDescription) {
        String generatedKey = getNameGenerator().getRandomNameForResource();
        testContext
                .given(PlatformEncryptionKeysTestDto.class)
                .withCredentialName(credentialName)
                .when(PlatformEncryptionKeysTestAction::getEncryptionKeys, key(generatedKey))
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
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("there is a running cloudbreak")
                                .when("calling get encryption keys endpoint with empty credential name")
                                .then("a BadRequestException should be returned")
                },
                {
                        getBean(MockedTestContext.class),
                        null,
                        BadRequestException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("there is a running cloudbreak")
                                .when("calling get encryption keys endpoint with 'null' credential name")
                                .then("a BadRequestException should be returned")
                },
                {
                        getBean(MockedTestContext.class),
                        "andNowForSomethingCompletelyDifferent",
                        ForbiddenException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("there is a running cloudbreak")
                                .when("calling get encryption keys endpoint with a non-existent credential name")
                                .then("a ForbiddenException should be returned")
                }
        };
    }

}

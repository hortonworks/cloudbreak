package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.assertion.credential.CredentialTestAssertion;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class CredentialTest extends AbstractIntegrationTest {

    private static final String BAD_REQUEST_KEY = "badRequest";

    private static final String INVALID_ATTRIBUTE_PROVIDER = "credentialInvalidAttirbutesTestProvider";

    @Inject
    private CredentialTestClient credentialTestClient;

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((MockedTestContext) data[0]).cleanupTestContext();
    }

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid credential create request is sent",
            then = "the credential can be found in the list credentials response"
    )
    public void testCredentialCreationWithCorrectRequest(MockedTestContext testContext) {
        String credentialName = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(credentialTestClient.createV4())
                .when(credentialTestClient.listV4())
                .then(CredentialTestAssertion.listContains(credentialName, 1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid credential create request is sent",
            then = "the credential can be found in the list credentials response"
    )
    public void testCreateDeleteCreate(MockedTestContext testContext) {
        String credentialName = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(credentialTestClient.createV4())
                .when(credentialTestClient.listV4())
                .then(CredentialTestAssertion.listContains(credentialName, 1))
                .when(credentialTestClient.deleteV4())
                .when(credentialTestClient.listV4())
                .then(CredentialTestAssertion.listContains(credentialName, 0))
                .when(credentialTestClient.createV4())
                .when(credentialTestClient.listV4())
                .then(CredentialTestAssertion.listContains(credentialName, 1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a previously created credential",
            when = "a credential create request with the same credential name is sent",
            then = "a BadRequestException should be returned"
    )
    public void testCreateTwice(MockedTestContext testContext) {
        String credentialName = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(credentialTestClient.createV4())
                .when(credentialTestClient.listV4())
                .then(CredentialTestAssertion.listContains(credentialName, 1))
                .withName(credentialName)
                .when(credentialTestClient.createV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a previously created credential",
            when = "a modifyV4 credential request is sent for that credential",
            then = "the credential is modified"
    )
    public void testModifyCredential(MockedTestContext testContext) {
        String credentialName = resourcePropertyProvider().getName();
        String modifiedDescription = "modified";
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(credentialTestClient.createV4())
                .when(credentialTestClient.listV4())
                .then(CredentialTestAssertion.listContains(credentialName, 1))
                .withName(credentialName)
                .withDescription(modifiedDescription)
                .when(credentialTestClient.modifyV4())
                .when(credentialTestClient.listV4())
                .then(CredentialTestAssertion.validateModifcation(modifiedDescription))
                .validate();
    }

    @Test(dataProvider = INVALID_ATTRIBUTE_PROVIDER)
    public void testCreateCredentialWithInvalidAttribute(
            MockedTestContext testContext,
            String credentialName,
            String expectedExceptionMessage,
            @Description TestCaseDescription testCaseDescription) {
        String requestKey = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(credentialTestClient.createV4(), key(requestKey))
                .expect(BadRequestException.class, expectedMessage(expectedExceptionMessage).withKey(requestKey))
                .validate();
    }

    @DataProvider(name = INVALID_ATTRIBUTE_PROVIDER)
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {
                        getBean(MockedTestContext.class),
                        getLongNameGenerator().stringGenerator(101),
                        "The length of the credential's name has to be in range of 5 to 100",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a credential with too long name")
                                .when("calling create credential")
                                .then("getting BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        "abc",
                        "The length of the credential's name has to be in range of 5 to 100",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a credential with too short name")
                                .when("calling create credential")
                                .then("getting BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        "a-@#$%|:&*;",
                        "The name of the credential can only contain lowercase alphanumeric characters and "
                                + "hyphens and has start with an alphanumeric character",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a credential with specific character in the name")
                                .when("calling create credential")
                                .then("getting BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        null,
                        "must not be null",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a credential with null name")
                                .when("calling create credential")
                                .then("getting BadRequestException")
                }
        };
    }
}

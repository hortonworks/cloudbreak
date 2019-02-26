package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.credential.CredentialTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.credential.CredentialTestAssertion;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class CredentialTest extends AbstractIntegrationTest {

    private static final String BAD_REQUEST_KEY = "badRequest";

    private static final String INVALID_ATTRIBUTE_PROVIDER = "credentialInvalidAttirbutesTestProvider";

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((MockedTestContext) data[0]).cleanupTestContextEntity();
    }

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCredentialCreationWithCorrectRequest(MockedTestContext testContext) {
        String credentialName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(CredentialTestAction::create)
                .when(CredentialTestAction::list)
                .then(CredentialTestAssertion.listContains(credentialName, 1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateDeleteCreate(MockedTestContext testContext) {
        String credentialName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(CredentialTestAction::create)
                .when(CredentialTestAction::list)
                .then(CredentialTestAssertion.listContains(credentialName, 1))
                .when(CredentialTestAction::delete)
                .when(CredentialTestAction::list)
                .then(CredentialTestAssertion.listContains(credentialName, 0))
                .when(CredentialTestAction::create)
                .when(CredentialTestAction::list)
                .then(CredentialTestAssertion.listContains(credentialName, 1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateTwice(MockedTestContext testContext) {
        String credentialName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(CredentialTestAction::create)
                .when(CredentialTestAction::list)
                .then(CredentialTestAssertion.listContains(credentialName, 1))
                .withName(credentialName)
                .when(CredentialTestAction::create, key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testModifyCredential(MockedTestContext testContext) {
        String credentialName = getNameGenerator().getRandomNameForResource();
        String modifiedDescription = "modified";
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(CredentialTestAction::create)
                .when(CredentialTestAction::list)
                .then(CredentialTestAssertion.listContains(credentialName, 1))
                .withName(credentialName)
                .withDescription(modifiedDescription)
                .when(CredentialTestAction::modify)
                .when(CredentialTestAction::list)
                .then(CredentialTestAssertion.validateModifcation(modifiedDescription))
                .validate();
    }

    @Test(dataProvider = INVALID_ATTRIBUTE_PROVIDER)
    public void testCreateCredentialWithInvalidAttribute(MockedTestContext testContext, String credentialName, String expectedExceptionMessage) {
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(CredentialTestAction::create, key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, expectedMessage(expectedExceptionMessage).withKey(BAD_REQUEST_KEY))
                .validate();
    }

    @DataProvider(name = INVALID_ATTRIBUTE_PROVIDER)
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {getBean(MockedTestContext.class), longStringGeneratorUtil.stringGenerator(101),
                        " The length of the credential's name has to be in range of 5 to 100"},
                {getBean(MockedTestContext.class), "abc",
                        " The length of the credential's name has to be in range of 5 to 100"},
                {getBean(MockedTestContext.class), "a-@#$%|:&*;",
                        " The name of the credential can only contain lowercase alphanumeric characters and "
                                + "hyphens and has start with an alphanumeric character"},
                {getBean(MockedTestContext.class), null,
                        "error: must not be null"}
        };
    }
}

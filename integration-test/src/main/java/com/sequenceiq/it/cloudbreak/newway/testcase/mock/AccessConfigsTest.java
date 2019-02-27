package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.accessconfig.PlatformAccessConfigsTestAction;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription.TestCaseDescriptionBuilder;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.accessconfig.PlatformAccessConfigsTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class AccessConfigsTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid MOCK credential",
            when = "calling get access config",
            then = "valid access config should be returned for MOCK")
    public void testGetAccessConfigsByCredentialName(MockedTestContext testContext) {
        String credentialName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(CredentialTestClient::create, key(credentialName))
                .given(PlatformAccessConfigsTestDto.class)
                .withCredentialName(credentialName)
                .when(PlatformAccessConfigsTestAction::getAccessConfigs, key(credentialName))
                .validate();
    }

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void testGetAccessConfigsByCredentialNameWhenCredentialIsInvalid(
            MockedTestContext testContext,
            String credentialName,
            Class<Exception> exception,
            @Description TestCaseDescription description) {
        String generatedKey = getNameGenerator().getRandomNameForResource();
        testContext
                .given(PlatformAccessConfigsTestDto.class)
                .withCredentialName(credentialName)
                .when(PlatformAccessConfigsTestAction::getAccessConfigs, key(generatedKey))
                .expect(exception, key(generatedKey))
                .validate();
    }

    @DataProvider(name = "contextWithCredentialNameAndException", parallel = true)
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {
                    getBean(MockedTestContext.class),
                    "",
                    BadRequestException.class,
                    new TestCaseDescriptionBuilder()
                        .given("a working Cloudbreak")
                        .when("get access config is called with empty credential name")
                        .then("returns with BadRequestException for MOCK")
                },
                {
                    getBean(MockedTestContext.class),
                    null,
                    BadRequestException.class,
                    new TestCaseDescriptionBuilder()
                        .given("a working Cloudbreak")
                        .when("get access config is called with null credential name")
                        .then("returns with BadRequestException for MOCK")
                },
                {
                    getBean(MockedTestContext.class),
                    "andNowForSomethingCompletelyDifferent",
                    ForbiddenException.class,
                    new TestCaseDescriptionBuilder()
                        .given("a working Cloudbreak")
                        .when("get access config is called with not existing credential name")
                        .then("returns with ForbiddenException for MOCK")}
        };
    }

}

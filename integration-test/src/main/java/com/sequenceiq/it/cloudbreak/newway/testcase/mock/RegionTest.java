package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.region.RegionTestAction;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.region.RegionTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class RegionTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a MOCK credential name",
            when = "calling get region on provider side",
            then = "getting back MOCK regions")
    public void testGetRegionsByCredentialName(MockedTestContext testContext) {
        String credentialName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(CredentialTestClient::create)
                .given(RegionTestDto.class)
                .withCredentialName(credentialName)
                .when(RegionTestAction::getRegions);
    }

    @Test(dataProvider = "contextWithCredentialNameAndException")
    public void testGetRegionsByCredentialNameWhenCredentialIsInvalid(
            MockedTestContext testContext,
            String credentialName,
            Class<Exception> exception,
            @Description TestCaseDescription testCaseDescription) {
        String exceptionKey = getNameGenerator().getRandomNameForResource();
        testContext
                .given(RegionTestDto.class)
                .withCredentialName(credentialName)
                .when(RegionTestAction::getRegions, key(exceptionKey))
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
                        ForbiddenException.class,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a MOCK credential name which does not belongs to that workspace")
                                .when("calling get region on provider side")
                                .then("getting ForbiddenException")
                }
        };
    }

}

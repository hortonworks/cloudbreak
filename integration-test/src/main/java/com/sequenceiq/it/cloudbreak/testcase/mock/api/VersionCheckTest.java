package com.sequenceiq.it.cloudbreak.testcase.mock.api;

import javax.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.CommonAssert;
import com.sequenceiq.it.cloudbreak.assertion.util.VersionCheckTestAssertion;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.VersionCheckTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class VersionCheckTest extends AbstractIntegrationTest {

    @Inject
    private UtilTestClient utilTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = "contextWithTestContextAndInvalidVersionValue")
    public void testGetVersionByInvalidVersionNumber(MockedTestContext testContext, String invalidVersionValue,
            @Description TestCaseDescription testCaseDescription) {
        testContext
                .given(VersionCheckTestDto.class)
                .withVersion(invalidVersionValue)
                .when(utilTestClient.versionChecker())
                .then(CommonAssert::responseExists)
                .then(VersionCheckTestAssertion.versionIsNotOk())
                .validate();
    }

    @DataProvider(name = "contextWithTestContextAndInvalidVersionValue")
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {
                        getBean(MockedTestContext.class),
                        "",
                        description("a version check", "version is empty", "version is not ok")
                },
                {
                        getBean(MockedTestContext.class),
                        "someOtherInvalidValue",
                        description("a version check", "version is invalid", "version is not ok")
                }
        };
    }

    private TestCaseDescription description(String given, String when, String then) {
        return new TestCaseDescription.TestCaseDescriptionBuilder()
                .given(given)
                .when(when)
                .then(then);
    }

}

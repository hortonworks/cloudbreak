package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.version.VersionCheckTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.CommonAssert;
import com.sequenceiq.it.cloudbreak.newway.assertion.version.VersionCheckAssertion;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.version.VersionCheckTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class VersionCheckTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = "contextWithTestContextAndInvalidVersionValue")
    public void testGetVersionByInvalidVersionNumber(MockedTestContext testContext, String invalidVersionValue) {
        testContext
                .given(VersionCheckTestDto.class)
                .withVersion(invalidVersionValue)
                .when(VersionCheckTestAction::getCheckClientVersion)
                .then(CommonAssert::responseExists)
                .then(VersionCheckAssertion::versionIsNotOk)
                .validate();
    }

    @DataProvider(name = "contextWithTestContextAndInvalidVersionValue")
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {getBean(MockedTestContext.class), ""},
                {getBean(MockedTestContext.class), "someOtherInvalidValue"}
        };
    }

}

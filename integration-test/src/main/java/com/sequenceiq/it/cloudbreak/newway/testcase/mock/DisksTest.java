package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.platformdisk.PlatformDisksTestAction;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.disk.PlatformDiskTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class DisksTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
        given = "there is a running cloudbreak",
        when = "calling get disk types endpoint",
        then = "the disk types should be returned")
    public void testGetPlatformDisks(MockedTestContext testContext) {
        testContext
                .given(PlatformDiskTestDto.class)
                .when(PlatformDisksTestAction::getDiskTypes)
                .validate();
    }

}

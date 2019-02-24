package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.notificationtest.NotificationTestingTestAction;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.notificationtest.NotificationTestingTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class NotificationTestingTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testPostNotificationTest(MockedTestContext testContext) {
        testContext
                .given(NotificationTestingTestDto.class)
                .when(NotificationTestingTestAction::postNotificationTest);
    }

}

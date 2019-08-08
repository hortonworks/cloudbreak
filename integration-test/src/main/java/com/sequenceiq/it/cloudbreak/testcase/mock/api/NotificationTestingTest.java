package com.sequenceiq.it.cloudbreak.testcase.mock.api;

import javax.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.NotificationTestingTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.NotificationTestingTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class NotificationTestingTest extends AbstractIntegrationTest {

    @Inject
    private NotificationTestingTestClient notificationTestingTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a notification test request",
            when = "posting to notification test",
            then = "getting back status ok")
    public void testPostNotificationTest(MockedTestContext testContext) {
        testContext
                .given(NotificationTestingTestDto.class)
                .when(notificationTestingTestClient.notificationTesting())
                .validate();
    }

}

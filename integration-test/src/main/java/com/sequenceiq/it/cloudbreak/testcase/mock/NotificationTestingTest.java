package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.NotificationTestingTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.util.NotificationTestingTestDto;

public class NotificationTestingTest extends AbstractMockTest {

    @Inject
    private NotificationTestingTestClient notificationTestingTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((MockedTestContext) data[0]);
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

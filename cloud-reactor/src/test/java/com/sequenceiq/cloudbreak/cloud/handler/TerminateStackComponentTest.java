package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollResourcesStateTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;

@SpringBootTest(classes = { TestApplicationContext.class, TerminateStackComponentTest.TestConfig.class },
        properties = "spring.main.allow-bean-definition-overriding=true")
class TerminateStackComponentTest extends AbstractComponentTest<TerminateStackResult> {

    @Test
    void testTerminateStack() {
        TerminateStackResult result = sendCloudRequest();

        assertEquals(EventStatus.OK, result.getStatus());
        assertNull(result.getErrorDetails());
    }

    @Override
    protected String getTopicName() {
        return "TERMINATESTACKREQUEST";
    }

    @Override
    protected CloudPlatformRequest<TerminateStackResult> getRequest() {
        return new TerminateStackRequest<>(g().createCloudContext(), g().createCloudStack(), g().createCloudCredential(), g().createCloudResourceList());
    }

    @Configuration
    @Import({TerminateStackHandler.class, PollTaskFactory.class, SyncPollingScheduler.class, PollResourcesStateTask.class})
    static class TestConfig {
    }

}

package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
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
public class TerminateStackComponentTest extends AbstractComponentTest<TerminateStackResult> {

    @Test
    public void testTerminateStack() {
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
    public static class TestConfig {
    }

}

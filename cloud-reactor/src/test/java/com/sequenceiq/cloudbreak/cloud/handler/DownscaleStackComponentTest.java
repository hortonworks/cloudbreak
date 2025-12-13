package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollResourcesStateTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;

@SpringBootTest(classes = { TestApplicationContext.class, DownscaleStackComponentTest.TestConfig.class },
        properties = "spring.main.allow-bean-definition-overriding=true")
class DownscaleStackComponentTest extends AbstractComponentTest<DownscaleStackResult> {

    @Test
    void testUpscaleStack() {
        DownscaleStackResult result = sendCloudRequest();

        assertEquals(EventStatus.OK, result.getStatus());
        assertEquals(1L, result.getDownscaledResources().size());
        assertNull(result.getErrorDetails());
    }

    @Override
    protected String getTopicName() {
        return "DOWNSCALESTACKREQUEST";
    }

    @Override
    protected CloudPlatformRequest<DownscaleStackResult> getRequest() {
        return new DownscaleStackRequest(
                g().createCloudContext(),
                g().createCloudCredential(),
                g().createCloudStack(),
                g().createCloudResourceList(),
                g().createCloudInstances());
    }

    @Configuration
    @Import({DownscaleStackHandler.class, SyncPollingScheduler.class, PollTaskFactory.class, PollResourcesStateTask.class})
    static class TestConfig {
    }
}

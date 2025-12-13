package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollResourcesStateTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.common.api.type.AdjustmentType;

@SpringBootTest(classes = { TestApplicationContext.class, LaunchStackComponentTest.TestConfig.class },
        properties = "spring.main.allow-bean-definition-overriding=true")
class LaunchStackComponentTest extends AbstractComponentTest<LaunchStackResult> {

    @MockBean
    private PersistenceNotifier persistenceNotifier;

    @Test
    void testLaunchStack() {
        LaunchStackResult lsr = sendCloudRequest();
        List<CloudResourceStatus> r = lsr.getResults();

        assertEquals(ResourceStatus.CREATED, r.get(0).getStatus());
        assertNull(lsr.getErrorDetails());
    }

    @Override
    protected String getTopicName() {
        return "LAUNCHSTACKREQUEST";
    }

    @Override
    protected CloudPlatformRequest<LaunchStackResult> getRequest() {
        return new LaunchStackRequest(g().createCloudContext(), g().createCloudCredential(), g().createCloudStack(), AdjustmentType.BEST_EFFORT, 0L);
    }

    @Configuration
    @Import({LaunchStackHandler.class, SyncPollingScheduler.class, PollTaskFactory.class, PollResourcesStateTask.class})
    static class TestConfig {
    }
}

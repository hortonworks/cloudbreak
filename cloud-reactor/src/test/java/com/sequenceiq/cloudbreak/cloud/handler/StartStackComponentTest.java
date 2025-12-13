package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@SpringBootTest(classes = { TestApplicationContext.class, StartStackComponentTest.TestConfig.class },
        properties = "spring.main.allow-bean-definition-overriding=true")
class StartStackComponentTest extends AbstractComponentTest<StartInstancesResult> {

    @Test
    void testStartStack() {
        StartInstancesResult result = sendCloudRequest();

        assertEquals(1L, result.getResults().getResults().size());
        assertEquals(InstanceStatus.STARTED, result.getResults().getResults().get(0).getStatus());
        assertNotEquals(result.getStatus(), EventStatus.FAILED);
        assertNull(result.getErrorDetails());
    }

    @Override
    protected String getTopicName() {
        return "STARTINSTANCESREQUEST";
    }

    @Override
    protected CloudPlatformRequest<StartInstancesResult> getRequest() {
        return new StartInstancesRequest(g().createCloudContext(), g().createCloudCredential(), g().createCloudResourceList(), g().createCloudInstances());
    }

    @Configuration
    @Import({StartStackHandler.class})
    static class TestConfig {
    }
}

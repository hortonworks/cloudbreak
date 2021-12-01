package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@SpringBootTest(classes = { TestApplicationContext.class, StopStackComponentTest.TestConfig.class },
        properties = "spring.main.allow-bean-definition-overriding=true")
public class StopStackComponentTest extends AbstractComponentTest<StopInstancesResult> {

    @Test
    public void testStopStack() {
        StopInstancesResult result = sendCloudRequest();

        assertEquals(1L, result.getResults().getResults().size());
        assertEquals(InstanceStatus.STOPPED, result.getResults().getResults().get(0).getStatus());
        assertNotEquals(result.getStatus(), EventStatus.FAILED);
        assertNull(result.getErrorDetails());
    }

    @Override
    protected String getTopicName() {
        return "STOPINSTANCESREQUEST";
    }

    @Override
    protected CloudPlatformRequest<StopInstancesResult> getRequest() {
        return new StopInstancesRequest<>(g().createCloudContext(), g().createCloudCredential(),
                g().createCloudResourceList(), g().createCloudInstances());
    }

    @Configuration
    @Import({StopStackHandler.class})
    public static class TestConfig {
    }
}

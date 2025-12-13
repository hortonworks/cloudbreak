package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@SpringBootTest(classes = { TestApplicationContext.class, InstanceStateComponentTest.TestConfig.class },
        properties = "spring.main.allow-bean-definition-overriding=true")
class InstanceStateComponentTest extends AbstractComponentTest<GetInstancesStateResult> {

    @Test
    void testInstanceState() {
        GetInstancesStateResult result = sendCloudRequest();

        assertEquals(InstanceStatus.STARTED, result.getStatuses().get(0).getStatus());
        assertFalse(result.isFailed());
        assertNull(result.getErrorDetails());
    }

    @Override
    protected String getTopicName() {
        return "GETINSTANCESSTATEREQUEST";
    }

    @Override
    protected CloudPlatformRequest<GetInstancesStateResult> getRequest() {
        return new GetInstancesStateRequest<>(g().createCloudContext(), g().createCloudCredential(), g().createCloudInstances());
    }

    @Configuration
    @Import({InstanceStateHandler.class, InstanceStateQuery.class})
    static class TestConfig {
    }
}

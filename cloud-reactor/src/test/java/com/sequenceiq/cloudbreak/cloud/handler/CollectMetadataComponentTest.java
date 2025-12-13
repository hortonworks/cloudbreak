package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@SpringBootTest(classes = { TestApplicationContext.class, CollectMetadataComponentTest.TestConfig.class },
        properties = "spring.main.allow-bean-definition-overriding=true")
class CollectMetadataComponentTest extends AbstractComponentTest<CollectMetadataResult> {

    @Test
    void testCollectMetadata() {
        CollectMetadataResult result = sendCloudRequest();

        assertEquals(1L, result.getResults().size());
        assertEquals(InstanceStatus.IN_PROGRESS, result.getResults().get(0).getCloudVmInstanceStatus().getStatus());
        assertNull(result.getErrorDetails());
    }

    @Override
    protected String getTopicName() {
        return "COLLECTMETADATAREQUEST";
    }

    @Override
    protected CloudPlatformRequest<CollectMetadataResult> getRequest() {
        return new CollectMetadataRequest(
                g().createCloudContext(),
                g().createCloudCredential(),
                g().createCloudResourceList(),
                g().createCloudInstances(),
                g().createCloudInstances());
    }

    @Configuration
    @Import({CollectMetadataHandler.class})
    static class TestConfig {
    }
}

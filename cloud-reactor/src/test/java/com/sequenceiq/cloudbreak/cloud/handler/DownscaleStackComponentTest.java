package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;

public class DownscaleStackComponentTest extends AbstractComponentTest<DownscaleStackResult> {

    @Test
    public void testUpscaleStack() {
        DownscaleStackResult result = sendCloudRequest();

        assertEquals(EventStatus.OK, result.getStatus());
        assertEquals(1, result.getDownscaledResources().size());
        assertNull(result.getErrorDetails());
    }

    @Override
    protected String getTopicName() {
        return "DOWNSCALESTACKREQUEST";
    }

    @Override
    protected CloudPlatformRequest getRequest() {
        return new DownscaleStackRequest(
                g().createCloudContext(),
                g().createCloudCredential(),
                g().createCloudStack(),
                g().createCloudResourceList(),
                g().createCloudInstances());
    }
}

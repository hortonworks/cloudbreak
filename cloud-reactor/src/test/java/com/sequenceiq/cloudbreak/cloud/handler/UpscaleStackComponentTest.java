package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class UpscaleStackComponentTest extends AbstractComponentTest<UpscaleStackResult> {

    @Test
    public void testUpscaleStack() {
        UpscaleStackResult result = sendCloudRequest();


        assertEquals(ResourceStatus.UPDATED, result.getStatus());
        assertEquals(1, result.getResults().size());
        assertEquals(ResourceStatus.UPDATED, result.getResults().get(0).getStatus());
        assertFalse(result.isFailed());
        assertNull(result.getException());
    }

    @Override
    protected String getTopicName() {
        return "UPSCALESTACKREQUEST";
    }

    @Override
    protected CloudPlatformRequest getRequest() {
        return new UpscaleStackRequest(g().createCloudContext(), g().createCloudCredential(), g().createCloudStack(), g().createCloudResourceList());
    }
}

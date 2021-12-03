package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;

public class UpscaleStackComponentTest extends AbstractComponentTest<UpscaleStackResult> {

    @Test
    public void testUpscaleStack() {
        UpscaleStackResult result = sendCloudRequest();


        assertEquals(ResourceStatus.UPDATED, result.getResourceStatus());
        assertEquals(1L, result.getResults().size());
        assertEquals(ResourceStatus.UPDATED, result.getResults().get(0).getStatus());
        assertFalse(result.isFailed());
        assertNull(result.getErrorDetails());
    }

    @Override
    protected String getTopicName() {
        return "UPSCALESTACKREQUEST";
    }

    @Override
    protected CloudPlatformRequest<UpscaleStackResult> getRequest() {
        return new UpscaleStackRequest<>(g().createCloudContext(), g().createCloudCredential(), g().createCloudStack(),
                g().createCloudResourceList(), new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, 5L));
    }
}

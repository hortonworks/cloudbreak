package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateStackResult;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class UpdateStackComponentTest extends AbstractComponentTest<UpdateStackResult> {

    @Test
    public void testUpdateStack() {
        UpdateStackResult result = sendCloudRequest();

        assertEquals(ResourceStatus.UPDATED, result.getStatus());
        assertEquals(1, result.getResults().size());
        assertEquals(ResourceStatus.UPDATED, result.getResults().get(0).getStatus());
        assertFalse(result.isFailed());
        assertNull(result.getException());
    }

    @Override
    protected String getTopicName() {
        return "UPDATESTACKREQUEST";
    }

    @Override
    protected CloudPlatformRequest getRequest() {
        return new UpdateStackRequest(g().createCloudContext(), g().createCloudCredential(), g().createCloudStack(), g().createCloudResourceList());
    }
}

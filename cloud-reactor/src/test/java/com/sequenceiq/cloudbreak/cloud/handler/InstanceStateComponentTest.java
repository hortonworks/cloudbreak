package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public class InstanceStateComponentTest extends AbstractComponentTest<GetInstancesStateResult> {

    @Test
    public void testInstanceState() {
        GetInstancesStateResult result = sendCloudRequest();

        assertEquals(InstanceStatus.STARTED, result.getStatuses().get(0).getStatus());
        assertFalse(result.isFailed());
        assertNull(result.getException());
    }

    @Override
    protected String getTopicName() {
        return "GETINSTANCESSTATEREQUEST";
    }

    @Override
    protected CloudPlatformRequest getRequest() {
        return new GetInstancesStateRequest(g().createCloudContext(), g().createCloudCredential(), g().createCloudInstances());
    }
}

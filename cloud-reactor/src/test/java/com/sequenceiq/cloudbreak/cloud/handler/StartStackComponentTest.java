package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public class StartStackComponentTest extends AbstractComponentTest<StartInstancesResult> {

    @Test
    public void testStartStack() {
        StartInstancesResult result = sendCloudRequest();

        assertEquals(1, result.getResults().getResults().size());
        assertEquals(InstanceStatus.STARTED, result.getResults().getResults().get(0).getStatus());
        assertFalse(result.isFailed());
        assertNull(result.getException());
    }

    @Override
    protected String getTopicName() {
        return "STARTINSTANCESREQUEST";
    }

    protected CloudPlatformRequest getRequest() {
        return new StartInstancesRequest(g().createCloudContext(), g().createCloudCredential(), g().createCloudResourceList(), g().createCloudInstances());
    }
}

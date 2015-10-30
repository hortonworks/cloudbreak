package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public class CollectMetadataComponentTest extends AbstractComponentTest<CollectMetadataResult> {

    @Test
    public void testCollectMetadata() {
        CollectMetadataResult result = sendCloudRequest();

        assertEquals(1, result.getResults().size());
        assertEquals(InstanceStatus.IN_PROGRESS, result.getResults().get(0).getCloudVmInstanceStatus().getStatus());
        assertNull(result.getException());
    }

    @Override
    protected String getTopicName() {
        return "COLLECTMETADATAREQUEST";
    }

    protected CloudPlatformRequest getRequest() {
        return new CollectMetadataRequest(
                g().createCloudContext(),
                g().createCloudCredential(),
                g().createCloudResourceList(),
                g().createCloudInstances());
    }
}

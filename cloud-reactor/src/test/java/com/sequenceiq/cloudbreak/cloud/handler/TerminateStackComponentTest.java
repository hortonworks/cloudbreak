package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;

public class TerminateStackComponentTest extends AbstractComponentTest<TerminateStackResult> {

    @Test
    public void testTerminateStack() {
        TerminateStackResult result = sendCloudRequest();

        assertEquals(EventStatus.OK, result.getStatus());
        assertNull(result.getErrorDetails());
    }

    @Override
    protected String getTopicName() {
        return "TERMINATESTACKREQUEST";
    }

    protected CloudPlatformRequest getRequest() {
        return new TerminateStackRequest(g().createCloudContext(), g().createCloudStack(), g().createCloudCredential(), g().createCloudResourceList());
    }
}

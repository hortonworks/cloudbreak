package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;

public class GetSSHFingerprintsComponentTest extends AbstractComponentTest<GetSSHFingerprintsResult> {
    @Inject
    @Qualifier("instance")
    private CloudInstance instance;

    @Inject
    @Qualifier("bad-instance")
    private CloudInstance instanceBad;

    @Test
    public void testGetSSHFingerprints() {
        GetSSHFingerprintsResult result = sendCloudRequest();

        assertEquals(EventStatus.OK, result.getStatus());
        assertTrue(result.getSshFingerprints().contains(g().getSshFingerprint()));
    }

    @Test
    public void testGetSSHFingerprintsWithBadFingerprint() {
        GetSSHFingerprintsResult result = sendCloudRequest(getBadRequest());

        assertEquals(EventStatus.FAILED, result.getStatus());
        assertNull(result.getSshFingerprints());
    }

    @Override
    protected String getTopicName() {
        return "GETSSHFINGERPRINTSREQUEST";
    }

    @Override
    protected CloudPlatformRequest getRequest() {
        return new GetSSHFingerprintsRequest(g().createCloudContext(), g().createCloudCredential(), instance, g().createCloudStack());
    }

    protected CloudPlatformRequest getBadRequest() {
        return new GetSSHFingerprintsRequest(g().createCloudContext(), g().createCloudCredential(), instanceBad, g().createCloudStack());
    }
}

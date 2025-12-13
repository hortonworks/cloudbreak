package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollInstanceConsoleOutputTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;

@SpringBootTest(classes = { TestApplicationContext.class, GetSSHFingerprintsComponentTest.TestConfig.class },
        properties = "spring.main.allow-bean-definition-overriding=true")
class GetSSHFingerprintsComponentTest extends AbstractComponentTest<GetSSHFingerprintsResult> {
    @Inject
    @Qualifier("instance")
    private CloudInstance instance;

    @Inject
    @Qualifier("bad-instance")
    private CloudInstance instanceBad;

    @Test
    void testGetSSHFingerprints() {
        GetSSHFingerprintsResult result = sendCloudRequest();

        assertEquals(EventStatus.OK, result.getStatus());
        assertTrue(result.getSshFingerprints().contains(g().getSshFingerprint()));
    }

    @Test
    void testGetSSHFingerprintsWithBadFingerprint() {
        GetSSHFingerprintsResult result = sendCloudRequest(getBadRequest());

        assertEquals(EventStatus.FAILED, result.getStatus());
        assertNull(result.getSshFingerprints());
    }

    @Override
    protected String getTopicName() {
        return "GETSSHFINGERPRINTSREQUEST";
    }

    @Override
    protected CloudPlatformRequest<GetSSHFingerprintsResult> getRequest() {
        return new GetSSHFingerprintsRequest<>(g().createCloudContext(), g().createCloudCredential(), instance);
    }

    protected CloudPlatformRequest<GetSSHFingerprintsResult> getBadRequest() {
        return new GetSSHFingerprintsRequest<>(g().createCloudContext(), g().createCloudCredential(), instanceBad);
    }

    @Configuration
    @Import({GetSSHFingerprintsHandler.class, PollTaskFactory.class, SyncPollingScheduler.class, PollInstanceConsoleOutputTask.class})
    static class TestConfig {
    }
}

package com.sequenceiq.cloudbreak.cloud.model.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

class EncryptionKeyEnableAutoRotationRequestTest {

    @Test
    void testBuilder() {
        CloudContext cloudContext = CloudContext.Builder.builder().build();
        CloudCredential cloudCredential = new CloudCredential();
        List<CloudResource> cloudResources = List.of();
        int rotationPeriodInDays = 90;

        EncryptionKeyEnableAutoRotationRequest underTest = EncryptionKeyEnableAutoRotationRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withCloudResources(cloudResources)
                .withRotationPeriodInDays(rotationPeriodInDays)
                .build();

        assertEquals(cloudContext, underTest.cloudContext());
        assertEquals(cloudCredential, underTest.cloudCredential());
        assertEquals(cloudResources, underTest.cloudResources());
        assertEquals(rotationPeriodInDays, underTest.rotationPeriodInDays());
    }
}

package com.sequenceiq.cloudbreak.cloud.model.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

class EncryptionKeyRotationRequestTest {

    @Test
    void builderTest() {
        CloudContext cloudContext = CloudContext.Builder.builder().build();
        CloudCredential cloudCredential = new CloudCredential();
        List<CloudResource> cloudResources = List.of();

        EncryptionKeyRotationRequest underTest = EncryptionKeyRotationRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withCloudResources(cloudResources)
                .build();

        assertEquals(cloudContext, underTest.cloudContext());
        assertEquals(cloudCredential, underTest.cloudCredential());
        assertEquals(cloudResources, underTest.cloudResources());
    }
}

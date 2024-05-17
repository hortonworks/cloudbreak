package com.sequenceiq.cloudbreak.cloud.model.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

class EncryptionKeyCreationRequestTest {

    @Test
    void builderTest() {
        CloudContext cloudContext = CloudContext.Builder.builder().build();
        CloudCredential cloudCredential = new CloudCredential();
        Map<String, String> tags = Map.of();
        List<CloudResource> cloudResources = List.of();
        List<String> cryptographicPrincipals = List.of();

        EncryptionKeyCreationRequest underTest = EncryptionKeyCreationRequest.builder()
                .withKeyName("keyName")
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withTags(tags)
                .withDescription("description")
                .withCloudResources(cloudResources)
                .withCryptographicPrincipals(cryptographicPrincipals)
                .build();

        assertThat(underTest.keyName()).isEqualTo("keyName");
        assertThat(underTest.cloudContext()).isSameAs(cloudContext);
        assertThat(underTest.cloudCredential()).isSameAs(cloudCredential);
        assertThat(underTest.tags()).isSameAs(tags);
        assertThat(underTest.description()).isEqualTo("description");
        assertThat(underTest.cloudResources()).isSameAs(cloudResources);
        assertThat(underTest.cryptographicPrincipals()).isSameAs(cryptographicPrincipals);
    }

}
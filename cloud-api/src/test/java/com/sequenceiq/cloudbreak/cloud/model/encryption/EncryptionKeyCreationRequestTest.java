package com.sequenceiq.cloudbreak.cloud.model.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

class EncryptionKeyCreationRequestTest {

    @Test
    void builderTest() {
        CloudContext cloudContext = CloudContext.Builder.builder().build();
        CloudCredential cloudCredential = new CloudCredential();
        Map<String, String> tags = Map.of();
        List<CloudResource> cloudResources = List.of();
        List<String> targetPrincipalIds = List.of();

        EncryptionKeyCreationRequest underTest = EncryptionKeyCreationRequest.builder()
                .withKeyName("keyName")
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withTags(tags)
                .withDescription("description")
                .withCloudResources(cloudResources)
                .withTargetPrincipalIds(targetPrincipalIds)
                .build();

        assertThat(underTest.keyName()).isEqualTo("keyName");
        assertThat(underTest.cloudContext()).isSameAs(cloudContext);
        assertThat(underTest.cloudCredential()).isSameAs(cloudCredential);
        assertThat(underTest.tags()).isSameAs(tags);
        assertThat(underTest.description()).isEqualTo("description");
        assertThat(underTest.cloudResources()).isSameAs(cloudResources);
        assertThat(underTest.targetPrincipalIds()).isSameAs(targetPrincipalIds);
    }

    @Test
    void platformTest() {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform("platform")
                .build();
        Platform platform = cloudContext.getPlatform();

        EncryptionKeyCreationRequest underTest = EncryptionKeyCreationRequest.builder()
                .withCloudContext(cloudContext)
                .build();

        assertThat(underTest.platform()).isSameAs(platform);
    }

    @Test
    void variantTest() {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withVariant("variant")
                .build();
        Variant variant = cloudContext.getVariant();

        EncryptionKeyCreationRequest underTest = EncryptionKeyCreationRequest.builder()
                .withCloudContext(cloudContext)
                .build();

        assertThat(underTest.variant()).isSameAs(variant);
    }

}
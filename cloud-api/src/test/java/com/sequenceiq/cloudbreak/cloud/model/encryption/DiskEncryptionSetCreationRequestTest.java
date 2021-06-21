package com.sequenceiq.cloudbreak.cloud.model.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@ExtendWith(MockitoExtension.class)
class DiskEncryptionSetCreationRequestTest {

    private static final String ENCRYPTION_KEY_URL = "mySecretKey";

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Test
    void toStringTestShouldNotIncludeEncryptionKeyUrl() {
        DiskEncryptionSetCreationRequest underTest = new DiskEncryptionSetCreationRequest.Builder()
                .withEncryptionKeyUrl(ENCRYPTION_KEY_URL)
                .withCloudContext(cloudContext)
                .withTags(Map.of())
                .withCloudCredential(cloudCredential)
                .withId("foo")
                .withDiskEncryptionSetResourceGroupName("myRg")
                .build();

        assertThat(underTest.getEncryptionKeyUrl()).isEqualTo(ENCRYPTION_KEY_URL);
        assertThat(underTest.toString()).doesNotContain(ENCRYPTION_KEY_URL);
    }

    @Test
    void testShouldCreateRequestWithEncryptionKeyUrlResourceGroupNameAndDiskEncryptionSetResourceGroupName() {
        DiskEncryptionSetCreationRequest underTest = new DiskEncryptionSetCreationRequest.Builder()
                .withEncryptionKeyUrl(ENCRYPTION_KEY_URL)
                .withCloudContext(cloudContext)
                .withTags(Map.of())
                .withCloudCredential(cloudCredential)
                .withId("foo")
                .withDiskEncryptionSetResourceGroupName("myRg")
                .withEncryptionKeyResourceGroupName("dummyRg")
                .build();

        assertThat(underTest.getDiskEncryptionSetResourceGroupName()).isEqualTo("myRg");
        assertThat(underTest.getEncryptionKeyResourceGroupName()).isEqualTo("dummyRg");
    }

}
package com.sequenceiq.cloudbreak.cloud;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.UpdateEncryptionKeyResourceAccessRequest;

class EncryptionResourcesTest {

    @Test
    void createDiskEncryptionSetTest() {
        EncryptionResources underTest = new DummyEncryptionResources();

        assertThrows(UnsupportedOperationException.class, () -> underTest.createDiskEncryptionSet(new DiskEncryptionSetCreationRequest.Builder().build()));
    }

    @Test
    void deleteDiskEncryptionSetTest() {
        EncryptionResources underTest = new DummyEncryptionResources();

        assertThrows(UnsupportedOperationException.class, () -> underTest.deleteDiskEncryptionSet(new DiskEncryptionSetDeletionRequest.Builder().build()));
    }

    @Test
    void createEncryptionKeyTest() {
        EncryptionResources underTest = new DummyEncryptionResources();

        assertThrows(UnsupportedOperationException.class, () -> underTest.createEncryptionKey(EncryptionKeyCreationRequest.builder().build()));
    }

    @Test
    void updateEncryptionKeyResourceAccessTest() {
        EncryptionResources underTest = new DummyEncryptionResources();

        assertThrows(UnsupportedOperationException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(UpdateEncryptionKeyResourceAccessRequest.builder().build()));
    }

    private static class DummyEncryptionResources implements EncryptionResources {

        @Override
        public Platform platform() {
            return Platform.platform("platform");
        }

        @Override
        public Variant variant() {
            return Variant.variant("variant");
        }
    }

}
package com.sequenceiq.cloudbreak.converter.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.old.AdlsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;

class CloudStorageValidationUtilTest {

    private final CloudStorageValidationUtil underTest = new CloudStorageValidationUtil();

    @Test
    void testIsCloudStorageConfiguredWhenCloudStorageNull() {
        boolean actual = underTest.isCloudStorageConfigured(null);

        assertFalse(actual);
    }

    @Test
    void testIsCloudStorageConfiguredWhenCloudStorageNotNull() {
        boolean actual = underTest.isCloudStorageConfigured(new CloudStorageRequest());

        assertFalse(actual);
    }

    @Test
    void testIsCloudStorageConfiguredWhenAdlsNotNull() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        StorageIdentityBase storageIdentityBase = new StorageIdentityBase();
        storageIdentityBase.setAdls(new AdlsCloudStorageV1Parameters());
        cloudStorageRequest.setIdentities(List.of(storageIdentityBase));
        cloudStorageRequest.setLocations(List.of(new StorageLocationBase()));

        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        assertTrue(actual);
    }

    @Test
    void testIsCloudStorageConfiguredWhenWasbNotNull() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        StorageIdentityBase storageIdentityBase = new StorageIdentityBase();
        storageIdentityBase.setWasb(new WasbCloudStorageV1Parameters());
        cloudStorageRequest.setIdentities(List.of(storageIdentityBase));
        cloudStorageRequest.setLocations(List.of(new StorageLocationBase()));

        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        assertTrue(actual);
    }

    @Test
    void testIsCloudStorageConfiguredWhenAdlsGen2NotNull() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        StorageIdentityBase storageIdentityBase = new StorageIdentityBase();
        storageIdentityBase.setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());
        cloudStorageRequest.setIdentities(List.of(storageIdentityBase));
        cloudStorageRequest.setLocations(List.of(new StorageLocationBase()));

        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        assertTrue(actual);
    }

    @Test
    void testIsCloudStorageConfiguredWhenS3NotNull() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        StorageIdentityBase storageIdentityBase = new StorageIdentityBase();
        storageIdentityBase.setS3(new S3CloudStorageV1Parameters());
        cloudStorageRequest.setIdentities(List.of(storageIdentityBase));
        cloudStorageRequest.setLocations(List.of(new StorageLocationBase()));

        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        assertTrue(actual);
    }

    @Test
    void testIsCloudStorageConfiguredWhenGcsNotNull() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        StorageIdentityBase storageIdentityBase = new StorageIdentityBase();
        storageIdentityBase.setGcs(new GcsCloudStorageV1Parameters());
        cloudStorageRequest.setIdentities(List.of(storageIdentityBase));
        cloudStorageRequest.setLocations(List.of(new StorageLocationBase()));

        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        assertTrue(actual);
    }
}

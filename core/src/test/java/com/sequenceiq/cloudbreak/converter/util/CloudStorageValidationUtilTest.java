package com.sequenceiq.cloudbreak.converter.util;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.old.AdlsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;

@RunWith(MockitoJUnitRunner.class)
public class CloudStorageValidationUtilTest {

    @InjectMocks
    private final CloudStorageValidationUtil underTest = new CloudStorageValidationUtil();

    @Test
    public void testIsCloudStorageConfiguredWhenCloudStorageNull() {
        boolean actual = underTest.isCloudStorageConfigured(null);

        Assert.assertFalse(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenCloudStorageNotNull() {
        boolean actual = underTest.isCloudStorageConfigured(new CloudStorageRequest());

        Assert.assertFalse(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenAdlsNotNull() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        StorageIdentityBase storageIdentityBase = new StorageIdentityBase();
        storageIdentityBase.setAdls(new AdlsCloudStorageV1Parameters());
        cloudStorageRequest.setIdentities(List.of(storageIdentityBase));
        cloudStorageRequest.setLocations(List.of(new StorageLocationBase()));

        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenWasbNotNull() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        StorageIdentityBase storageIdentityBase = new StorageIdentityBase();
        storageIdentityBase.setWasb(new WasbCloudStorageV1Parameters());
        cloudStorageRequest.setIdentities(List.of(storageIdentityBase));
        cloudStorageRequest.setLocations(List.of(new StorageLocationBase()));

        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenAdlsGen2NotNull() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        StorageIdentityBase storageIdentityBase = new StorageIdentityBase();
        storageIdentityBase.setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());
        cloudStorageRequest.setIdentities(List.of(storageIdentityBase));
        cloudStorageRequest.setLocations(List.of(new StorageLocationBase()));

        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenS3NotNull() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        StorageIdentityBase storageIdentityBase = new StorageIdentityBase();
        storageIdentityBase.setS3(new S3CloudStorageV1Parameters());
        cloudStorageRequest.setIdentities(List.of(storageIdentityBase));
        cloudStorageRequest.setLocations(List.of(new StorageLocationBase()));

        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenGcsNotNull() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        StorageIdentityBase storageIdentityBase = new StorageIdentityBase();
        storageIdentityBase.setGcs(new GcsCloudStorageV1Parameters());
        cloudStorageRequest.setIdentities(List.of(storageIdentityBase));
        cloudStorageRequest.setLocations(List.of(new StorageLocationBase()));

        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }
}

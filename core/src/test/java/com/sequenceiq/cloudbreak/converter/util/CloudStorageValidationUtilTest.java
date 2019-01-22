package com.sequenceiq.cloudbreak.converter.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.AdlsCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.AdlsGen2CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.WasbCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.gcs.GcsCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.s3.S3CloudStorageParametersV4;

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
        boolean actual = underTest.isCloudStorageConfigured(new CloudStorageV4Request());

        Assert.assertFalse(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenAdlsNotNull() {
        CloudStorageV4Request cloudStorageRequest = new CloudStorageV4Request();
        cloudStorageRequest.setAdls(new AdlsCloudStorageParametersV4());
        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenWasbNotNull() {
        CloudStorageV4Request cloudStorageRequest = new CloudStorageV4Request();
        cloudStorageRequest.setWasb(new WasbCloudStorageParametersV4());
        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenAdlsGen2NotNull() {
        CloudStorageV4Request cloudStorageRequest = new CloudStorageV4Request();
        cloudStorageRequest.setAdlsGen2(new AdlsGen2CloudStorageParametersV4());
        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenS3NotNull() {
        CloudStorageV4Request cloudStorageRequest = new CloudStorageV4Request();
        cloudStorageRequest.setS3(new S3CloudStorageParametersV4());
        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenGcsNotNull() {
        CloudStorageV4Request cloudStorageRequest = new CloudStorageV4Request();
        cloudStorageRequest.setGcs(new GcsCloudStorageParametersV4());
        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }
}

package com.sequenceiq.cloudbreak.converter.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsGen2CloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.WasbCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;

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
        cloudStorageRequest.setAdls(new AdlsCloudStorageParameters());
        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenWasbNotNull() {
        CloudStorageV4Request cloudStorageRequest = new CloudStorageV4Request();
        cloudStorageRequest.setWasb(new WasbCloudStorageParameters());
        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenAdlsGen2NotNull() {
        CloudStorageV4Request cloudStorageRequest = new CloudStorageV4Request();
        cloudStorageRequest.setAdlsGen2(new AdlsGen2CloudStorageParameters());
        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenS3NotNull() {
        CloudStorageV4Request cloudStorageRequest = new CloudStorageV4Request();
        cloudStorageRequest.setS3(new S3CloudStorageParameters());
        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsCloudStorageConfiguredWhenGcsNotNull() {
        CloudStorageV4Request cloudStorageRequest = new CloudStorageV4Request();
        cloudStorageRequest.setGcs(new GcsCloudStorageParameters());
        boolean actual = underTest.isCloudStorageConfigured(cloudStorageRequest);

        Assert.assertTrue(actual);
    }
}

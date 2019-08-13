package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;

@RunWith(MockitoJUnitRunner.class)
public class AwsObjectStorageConnectorTest {

    private static final String BUCKET_NAME = "mybucket";

    private static final String REGION_NAME = "bucket-location";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private AwsClient awsClient;

    @Mock
    private AmazonS3 s3Client;

    @InjectMocks
    private AwsObjectStorageConnector underTest;

    @Before
    public void setUp() {
        when(awsClient.createS3Client(any(AwsCredentialView.class))).thenReturn(s3Client);
    }

    @Test
    public void getObjectStorageMetadata() {
        when(s3Client.getBucketLocation(BUCKET_NAME)).thenReturn(REGION_NAME);
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(BUCKET_NAME).build();
        ObjectStorageMetadataResponse result = underTest.getObjectStorageMetadata(new CloudCredential("id", "name"), request);
        verify(s3Client).getBucketLocation(BUCKET_NAME);
        assertEquals(REGION_NAME, result.getRegion());
    }

    @Test
    public void getObjectStorageMetadataThrows() {
        AmazonS3Exception exception = new AmazonS3Exception("errormessage");
        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("errormessage");
        when(s3Client.getBucketLocation(BUCKET_NAME)).thenThrow(exception);
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(BUCKET_NAME).build();
        underTest.getObjectStorageMetadata(new CloudCredential("id", "name"), request);
    }

    @Test
    public void platform() {
        assertEquals(AwsConstants.AWS_PLATFORM, underTest.platform());
    }

    @Test
    public void variant() {
        assertEquals(AwsConstants.AWS_VARIANT, underTest.variant());
    }
}

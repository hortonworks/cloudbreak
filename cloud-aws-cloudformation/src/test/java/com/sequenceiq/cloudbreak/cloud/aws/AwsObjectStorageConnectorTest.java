package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonS3Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;

@RunWith(MockitoJUnitRunner.class)
public class AwsObjectStorageConnectorTest {

    private static final String BUCKET_NAME = "mybucket";

    private static final String REGION_NAME = "bucket-location";

    private static final String ERROR_MESSAGE = "errormessage";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private AwsClient awsClient;

    @Mock
    private AmazonS3Client s3Client;

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
        ObjectStorageMetadataResponse result = underTest.getObjectStorageMetadata(request);
        verify(s3Client).getBucketLocation(BUCKET_NAME);
        assertEquals(REGION_NAME, result.getRegion());
        assertEquals(ResponseStatus.OK, result.getStatus());
    }

    @Test
    public void getObjectStorageMetadataAccessDenied() {
        AmazonS3Exception exception = new AmazonS3Exception(ERROR_MESSAGE);
        exception.setStatusCode(403);
        when(s3Client.getBucketLocation(BUCKET_NAME)).thenThrow(exception);
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(BUCKET_NAME).build();
        ObjectStorageMetadataResponse result = underTest.getObjectStorageMetadata(request);
        verify(s3Client).getBucketLocation(BUCKET_NAME);
        assertNull(result.getRegion());
        assertEquals(ResponseStatus.ACCESS_DENIED, result.getStatus());
    }

    @Test
    public void getObjectStorageMetadataThrows() {
        AmazonS3Exception exception = new AmazonS3Exception(ERROR_MESSAGE);
        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("We were not able to query S3 object storage location for mybucket. " +
                "Refer to Cloudera documentation at " +
                DocumentationLinkProvider.awsCloudStorageSetupLink() +
                " for the required setup. The message from Amazon S3 was: errormessage.");
        when(s3Client.getBucketLocation(BUCKET_NAME)).thenThrow(exception);
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(BUCKET_NAME).build();
        underTest.getObjectStorageMetadata(request);
    }

    @Test
    public void platform() {
        assertEquals(AwsConstants.AWS_PLATFORM, underTest.platform());
    }

    @Test
    public void variant() {
        assertEquals(AwsConstants.AWS_DEFAULT_VARIANT, underTest.variant());
    }
}

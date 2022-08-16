package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonS3Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsCloudWatchCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;

@ExtendWith(MockitoExtension.class)
public class AwsObjectStorageConnectorTest {

    private static final String BUCKET_NAME = "mybucket";

    private static final String REGION_NAME = "bucket-location";

    private static final String ERROR_MESSAGE = "errormessage";

    private static final double DOUBLE_ASSERT_EPSILON = 0.001;

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AmazonS3Client s3Client;

    @Mock
    private AwsCloudWatchCommonService cloudWatchCommonService;

    @InjectMocks
    private AwsObjectStorageConnector underTest;

    @Test
    public void getObjectStorageMetadata() {
        when(awsClient.createS3Client(any(AwsCredentialView.class))).thenReturn(s3Client);
        when(s3Client.getBucketLocation(BUCKET_NAME)).thenReturn(REGION_NAME);
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(BUCKET_NAME).build();
        ObjectStorageMetadataResponse result = underTest.getObjectStorageMetadata(request);
        verify(s3Client).getBucketLocation(BUCKET_NAME);
        assertEquals(REGION_NAME, result.getRegion());
        assertEquals(ResponseStatus.OK, result.getStatus());
    }

    @Test
    public void getObjectStorageMetadataAccessDenied() {
        when(awsClient.createS3Client(any(AwsCredentialView.class))).thenReturn(s3Client);
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
        when(awsClient.createS3Client(any(AwsCredentialView.class))).thenReturn(s3Client);
        AmazonS3Exception exception = new AmazonS3Exception(ERROR_MESSAGE);
        when(s3Client.getBucketLocation(BUCKET_NAME)).thenThrow(exception);
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(BUCKET_NAME).build();
        CloudConnectorException ex = assertThrows(CloudConnectorException.class, () -> underTest.getObjectStorageMetadata(request));
        assertEquals(String.format("We were not able to query S3 object storage location for %s. "
                        + "Refer to Cloudera documentation at %s for the required setup. "
                        + "The message from Amazon S3 was: %s.",
                BUCKET_NAME, DocumentationLinkProvider.awsCloudStorageSetupLink(), ERROR_MESSAGE), ex.getMessage());
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

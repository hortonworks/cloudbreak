package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonS3Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsCloudWatchCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.cloudbreak.util.S3ExpressBucketNameValidator;
import com.sequenceiq.common.model.ObjectStorageType;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class AwsObjectStorageConnectorTest {

    private static final String BUCKET_NAME = "mybucket";

    private static final String EXPRESS_BUCKET_NAME = "mybucket--x-s3";

    private static final String REGION_NAME = "bucket-location";

    private static final String ERROR_MESSAGE = "errormessage";

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AmazonS3Client s3Client;

    @Mock
    private AwsCloudWatchCommonService cloudWatchCommonService;

    @Mock
    private S3ExpressBucketNameValidator s3ExpressBucketNameValidator;

    @InjectMocks
    private AwsObjectStorageConnector underTest;

    void setUp() {
        ReflectionTestUtils.setField(s3ExpressBucketNameValidator, "s3expressBucketSuffixPattern", "--x-s3");
        doCallRealMethod().when(s3ExpressBucketNameValidator).isS3ExpressBucket(anyString());
    }

    @Test
    void getObjectStorageMetadata() {
        when(awsClient.createS3Client(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(s3Client);
        when(s3Client.getBucketLocation(BUCKET_NAME)).thenReturn(REGION_NAME);
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(BUCKET_NAME).withRegion(REGION_NAME).build();
        ObjectStorageMetadataResponse result = underTest.getObjectStorageMetadata(request);
        verify(s3Client).getBucketLocation(BUCKET_NAME);
        assertEquals(REGION_NAME, result.getRegion());
        assertEquals(ResponseStatus.OK, result.getStatus());
    }

    @Test
    void getObjectStorageMetadataAccessDenied() {
        when(awsClient.createS3Client(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(s3Client);
        S3Exception exception = (S3Exception) S3Exception.builder()
                .message(ERROR_MESSAGE)
                .statusCode(403)
                .build();
        when(s3Client.getBucketLocation(BUCKET_NAME)).thenThrow(exception);
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(BUCKET_NAME).withRegion(REGION_NAME).build();
        ObjectStorageMetadataResponse result = underTest.getObjectStorageMetadata(request);
        verify(s3Client).getBucketLocation(BUCKET_NAME);
        assertNull(result.getRegion());
        assertEquals(ResponseStatus.ACCESS_DENIED, result.getStatus());
    }

    @Test
    void getObjectStorageMetadataClientException() {
        when(awsClient.createS3Client(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(s3Client);
        SdkClientException exception = SdkClientException.builder()
                .message(ERROR_MESSAGE)
                .build();
        when(s3Client.getBucketLocation(BUCKET_NAME)).thenThrow(exception);
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(BUCKET_NAME).withRegion(REGION_NAME).build();
        assertThatThrownBy(() -> underTest.getObjectStorageMetadata(request))
                .isInstanceOf(CloudConnectorException.class)
                .hasMessage(ERROR_MESSAGE);
        verify(s3Client).getBucketLocation(BUCKET_NAME);
    }

    @Test
    void getObjectStorageMetadataThrows() {
        when(awsClient.createS3Client(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(s3Client);
        S3Exception exception = (S3Exception) S3Exception.builder()
                .statusCode(500)
                .awsErrorDetails(AwsErrorDetails.builder().errorMessage(ERROR_MESSAGE).build())
                .build();
        when(s3Client.getBucketLocation(BUCKET_NAME)).thenThrow(exception);
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(BUCKET_NAME).withRegion(REGION_NAME).build();
        CloudConnectorException ex = assertThrows(CloudConnectorException.class, () -> underTest.getObjectStorageMetadata(request));
        assertEquals(String.format("We were not able to query S3 object storage location for %s. "
                        + "Refer to Cloudera documentation at %s for the required setup. "
                        + "The message from Amazon S3 was: %s.",
                BUCKET_NAME, DocumentationLinkProvider.awsCloudStorageSetupLink(), ERROR_MESSAGE), ex.getMessage());
    }

    @Test
    void getObjectStorageMetadataThrowsBadRequestForLogsStorageType() {
        setUp();
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(EXPRESS_BUCKET_NAME)
                .withRegion(REGION_NAME).withObjectStorageType(ObjectStorageType.LOGS).build();
        BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest.getObjectStorageMetadata(request));
        assertEquals("S3 Express storage isn't supported for LOGS location. Bucket name: mybucket--x-s3", ex.getMessage());
    }

    @Test
    void getObjectStorageMetadataThrowsBadRequestForBackupStorageType() {
        setUp();
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(EXPRESS_BUCKET_NAME)
                .withRegion(REGION_NAME).withObjectStorageType(ObjectStorageType.BACKUP).build();
        BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest.getObjectStorageMetadata(request));
        assertEquals("S3 Express storage isn't supported for BACKUP location. Bucket name: mybucket--x-s3", ex.getMessage());
    }

    @Test
    void getObjectStorageMetadataValidS3Express() {
        setUp();
        when(awsClient.createS3Client(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(s3Client);
        when(s3Client.getBucketLocation(EXPRESS_BUCKET_NAME)).thenReturn(REGION_NAME);
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(EXPRESS_BUCKET_NAME)
                .withRegion(REGION_NAME).withObjectStorageType(ObjectStorageType.DATALAKE).build();
        ObjectStorageMetadataResponse result = underTest.getObjectStorageMetadata(request);
        verify(s3Client).getBucketLocation(EXPRESS_BUCKET_NAME);
        assertEquals(REGION_NAME, result.getRegion());
        assertEquals(ResponseStatus.OK, result.getStatus());
    }

    @Test
    void platform() {
        assertEquals(AwsConstants.AWS_PLATFORM, underTest.platform());
    }

    @Test
    void variant() {
        assertEquals(AwsConstants.AWS_DEFAULT_VARIANT, underTest.variant());
    }
}

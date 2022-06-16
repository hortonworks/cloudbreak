package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonS3Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsCloudWatchCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageSizeRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageSizeResponse;
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
    public void getObjectStorageSizeLatestDatapointIsUsed() {
        Date startTime = Date.from(Instant.now().minus(42, ChronoUnit.MINUTES));
        Date endTime = Date.from(Instant.now());
        Region region = Region.region(REGION_NAME);
        ObjectStorageSizeRequest request = ObjectStorageSizeRequest.builder()
                .withObjectStoragePath(BUCKET_NAME)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withRegion(region)
                .build();

        Instant latestTimestamp = Instant.now();
        Datapoint latestDatapoint = new Datapoint()
                .withTimestamp(Date.from(latestTimestamp))
                .withMaximum(42.0);
        Datapoint earlierDatapoint = new Datapoint()
                .withTimestamp(Date.from(latestTimestamp.minus(1, ChronoUnit.DAYS)))
                .withMaximum(21.0);
        Datapoint earliestDatapoint = new Datapoint()
                .withTimestamp(Date.from(latestTimestamp.minus(2, ChronoUnit.DAYS)))
                .withMaximum(10.5);
        GetMetricStatisticsResult statisticsResult = new GetMetricStatisticsResult()
                .withDatapoints(List.of(latestDatapoint, earlierDatapoint, earliestDatapoint));
        when(cloudWatchCommonService.getBucketSize(null, REGION_NAME, startTime, endTime, BUCKET_NAME)).thenReturn(statisticsResult);

        ObjectStorageSizeResponse result = underTest.getObjectStorageSize(request);

        verify(cloudWatchCommonService).getBucketSize(null, REGION_NAME, startTime, endTime, BUCKET_NAME);
        assertEquals(42.0, result.getStorageInBytes(), DOUBLE_ASSERT_EPSILON);
    }

    @Test
    public void getObjectStorageSizeOneDatapoint() {
        Date startTime = Date.from(Instant.now().minus(42, ChronoUnit.MINUTES));
        Date endTime = Date.from(Instant.now());
        Region region = Region.region(REGION_NAME);
        ObjectStorageSizeRequest request = ObjectStorageSizeRequest.builder()
                .withObjectStoragePath(BUCKET_NAME)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withRegion(region)
                .build();

        Datapoint datapoint = new Datapoint()
                .withTimestamp(Date.from(Instant.now()))
                .withMaximum(42.0);
        GetMetricStatisticsResult statisticsResult = new GetMetricStatisticsResult()
                .withDatapoints(List.of(datapoint));
        when(cloudWatchCommonService.getBucketSize(null, REGION_NAME, startTime, endTime, BUCKET_NAME)).thenReturn(statisticsResult);

        ObjectStorageSizeResponse result = underTest.getObjectStorageSize(request);

        verify(cloudWatchCommonService).getBucketSize(null, REGION_NAME, startTime, endTime, BUCKET_NAME);
        assertEquals(42.0, result.getStorageInBytes(), DOUBLE_ASSERT_EPSILON);
    }

    @Test
    public void getObjectStorageNoDatapointThrowsException() {
        Date startTime = Date.from(Instant.now().minus(42, ChronoUnit.MINUTES));
        Date endTime = Date.from(Instant.now());
        Region region = Region.region(REGION_NAME);
        ObjectStorageSizeRequest request = ObjectStorageSizeRequest.builder()
                .withObjectStoragePath(BUCKET_NAME)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withRegion(region)
                .build();

        GetMetricStatisticsResult statisticsResult = new GetMetricStatisticsResult()
                .withDatapoints(List.of());
        when(cloudWatchCommonService.getBucketSize(null, REGION_NAME, startTime, endTime, BUCKET_NAME)).thenReturn(statisticsResult);

        CloudConnectorException ex = assertThrows(CloudConnectorException.class, () -> underTest.getObjectStorageSize(request));

        verify(cloudWatchCommonService).getBucketSize(null, REGION_NAME, startTime, endTime, BUCKET_NAME);
        assertEquals(String.format("No datapoints were returned by CloudWatch for bucket %s and timeframe from %s to %s",
                BUCKET_NAME, startTime, endTime), ex.getMessage());
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

package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

@ExtendWith(MockitoExtension.class)
public class AwsCloudWatchCommonServiceTest {

    private static final String REGION = "region";

    private static final String BUCKET_NAME = "bucket";

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private AmazonCloudWatchClient amazonCloudWatchClient;

    @InjectMocks
    private AwsCloudWatchCommonService underTest;

    @Captor
    private ArgumentCaptor<GetMetricStatisticsRequest> requestCaptor;

    private Date startTime;

    private Date endTime;

    @BeforeEach
    public void setUp() {
        startTime = Date.from(Instant.now().minus(42, ChronoUnit.MINUTES));
        endTime = Date.from(Instant.now());
    }

    @Test
    public void getBucketSizeCloudWatchClientThrowsAmazonCloudWatchException() {
        when(awsClient.createCloudWatchClient(any(), eq(REGION))).thenReturn(amazonCloudWatchClient);
        CloudWatchException cwException = (CloudWatchException) CloudWatchException.builder().message("CW error").build();
        when(amazonCloudWatchClient.getMetricStatisticsResponse(any())).thenThrow(cwException);

        CloudConnectorException ex = assertThrows(CloudConnectorException.class,
                () -> underTest.getBucketSize(cloudCredential, REGION, startTime, endTime, BUCKET_NAME));

        assertEquals(String.format("Cannot get BucketSizeBytes for bucket %s and timeframe from %s to %s. Reason: %s",
                BUCKET_NAME, startTime, endTime, cwException.getMessage()), ex.getMessage());
        assertEquals(cwException, ex.getCause());
    }

    @Test
    public void getBucketSizeAwsClientThrowsOtherException() {
        when(awsClient.createCloudWatchClient(any(), eq(REGION))).thenThrow(new RuntimeException("Other error"));

        Exception ex = assertThrows(Exception.class, () -> underTest.getBucketSize(cloudCredential, REGION, startTime, endTime, BUCKET_NAME));

        assertEquals("Other error", ex.getMessage());
    }

    @Test
    public void getBucketSizeWorksCorrectly() {
        GetMetricStatisticsResponse result = GetMetricStatisticsResponse.builder().build();

        when(awsClient.createCloudWatchClient(any(), eq(REGION))).thenReturn(amazonCloudWatchClient);
        when(amazonCloudWatchClient.getMetricStatisticsResponse(any())).thenReturn(result);

        GetMetricStatisticsResponse returnedResult = underTest.getBucketSize(cloudCredential, REGION, startTime, endTime, BUCKET_NAME);

        assertEquals(result, returnedResult);
        verify(amazonCloudWatchClient, times(1)).getMetricStatisticsResponse(requestCaptor.capture());

        GetMetricStatisticsRequest request = requestCaptor.getValue();
        assertEquals(3600, request.period());
        assertEquals("BucketSizeBytes", request.metricName());
        assertEquals("Bytes", request.unit().toString());
        assertEquals("AWS/S3", request.namespace());
        assertEquals(startTime.toInstant(), request.startTime());
        assertEquals(endTime.toInstant(), request.endTime());
        assertTrue(request.statistics().contains(Statistic.MAXIMUM));
        assertTrue(request.dimensions().stream()
                .filter(dimension -> "StorageType".equals(dimension.name()))
                .anyMatch(dimension -> "StandardStorage".equals(dimension.value())));
        assertTrue(request.dimensions().stream()
                .filter(dimension -> "BucketName".equals(dimension.name()))
                .anyMatch(dimension -> BUCKET_NAME.equals(dimension.value())));
    }
}

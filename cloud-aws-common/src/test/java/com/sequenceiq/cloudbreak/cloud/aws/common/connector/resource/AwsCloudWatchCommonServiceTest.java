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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.cloudwatch.model.AmazonCloudWatchException;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

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

    @Mock
    private GetMetricStatisticsResult result;

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
        AmazonCloudWatchException cwException = new AmazonCloudWatchException("CW error");
        when(amazonCloudWatchClient.getMetricStatisticsResult(any())).thenThrow(cwException);

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
        when(awsClient.createCloudWatchClient(any(), eq(REGION))).thenReturn(amazonCloudWatchClient);
        when(amazonCloudWatchClient.getMetricStatisticsResult(any())).thenReturn(result);

        GetMetricStatisticsResult returnedResult = underTest.getBucketSize(cloudCredential, REGION, startTime, endTime, BUCKET_NAME);

        assertEquals(result, returnedResult);
        verify(amazonCloudWatchClient, times(1)).getMetricStatisticsResult(requestCaptor.capture());

        GetMetricStatisticsRequest request = requestCaptor.getValue();
        assertEquals(3600, request.getPeriod());
        assertEquals("BucketSizeBytes", request.getMetricName());
        assertEquals("Bytes", request.getUnit());
        assertEquals("AWS/S3", request.getNamespace());
        assertEquals(startTime, request.getStartTime());
        assertEquals(endTime, request.getEndTime());
        assertEquals(List.of("Maximum"), request.getStatistics());
        assertTrue(request.getDimensions().stream()
                .filter(dimension -> "StorageType".equals(dimension.getName()))
                .anyMatch(dimension -> "StandardStorage".equals(dimension.getValue())));
        assertTrue(request.getDimensions().stream()
                .filter(dimension -> "BucketName".equals(dimension.getName()))
                .anyMatch(dimension -> BUCKET_NAME.equals(dimension.getValue())));
    }
}

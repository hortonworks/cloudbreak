package com.sequenceiq.consumption.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.BadRequestException;

import com.amazonaws.services.cloudwatch.model.AmazonCloudWatchException;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class CloudWatchServiceTest {

    private static final String REGION = "eu";

    private static final int PERIOD = 3600;

    private static final String METRIC_NAME = "metricname";

    private static final String UNIT = "Bytes";

    private static final String NAMESPACE = "AWS/S3";

    private static final Date START_TIME = new Date(2022,  6, 6);

    private static final Date END_TIME = new Date(2022, 6, 8);

    private static final String STATISTICS_TYPE = "Maximum";

    private static final String BUCKET_NAME = "test";

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private AmazonCloudWatchClient cloudWatchClient;

    @InjectMocks
    private CloudWatchService underTest;

    private GetMetricStatisticsRequest request;

    private List<Dimension> dimensionList;

    private GetMetricStatisticsResult result;

    private Dimension bucketDimension;

    private Dimension storageDimension;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        result = new GetMetricStatisticsResult();
        when(awsClient.createCloudWatchClient(any(), eq(REGION))).thenReturn(cloudWatchClient);
        when(cloudWatchClient.getMetricStatisticsResult(request)).thenReturn(result);
        bucketDimension = new Dimension().withName("BucketName").withValue(BUCKET_NAME);
        storageDimension =  new Dimension().withName("StorageType").withValue("StandardStorage");

        storageDimension.setValue("StandardStorage");
        dimensionList = new ArrayList<>(Arrays.asList(bucketDimension, storageDimension));
        request = new GetMetricStatisticsRequest()
                .withPeriod(PERIOD)
                .withMetricName(METRIC_NAME)
                .withUnit(UNIT)
                .withNamespace(NAMESPACE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withStatistics(STATISTICS_TYPE)
                .withDimensions(dimensionList);
    }

    @Test
    public void getMetricsStatistics() {
        when(cloudWatchClient.getMetricStatisticsResult(request)).thenReturn(result);
        assertEquals(result, underTest.getMetricsStatistics(cloudCredential, REGION, METRIC_NAME, START_TIME, END_TIME, dimensionList, PERIOD));
    }

    @Test
    public void getMetricsStatisticsNotfound() {
        when(cloudWatchClient.getMetricStatisticsResult(request)).thenThrow(AmazonCloudWatchException.class);
        assertThrows(BadRequestException.class, () -> underTest.getMetricsStatistics(cloudCredential,
                REGION, METRIC_NAME, START_TIME, END_TIME, dimensionList, PERIOD));
    }
}

package com.sequenceiq.consumption.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;


import org.springframework.stereotype.Service;

@Service
public class CloudWatchService {

    private static final String BUCKET_NAME_DIMENSION = "BucketName";

    private static final String STORAGE_TYPE = "StandardStorage";

    private static final String STORAGE_TYPE_DIMENSION = "StorageType";

    private static final String NAMESPACE = "AWS/S3";

    private static final String METRIC_NAME = "BucketSizeBytes";

    private static final String STATISTICS_TYPE = "Maximum";

    private static final String UNIT = "Bytes";

    private static final int PERIOD = 3600;

    @Inject
    private AwsCloudFormationClient awsClient;

    public GetMetricStatisticsResult getMetricsStatistics(CloudCredential cloudcredential, String region, String metricName, Date startTime, Date endTime,
            List<Dimension> dimensions, Integer period) {
        AwsCredentialView credentialView = new AwsCredentialView(cloudcredential);
        AmazonCloudWatchClient amazonCloudWatchClient = awsClient.createCloudWatchClient(credentialView, region);
        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withPeriod(period)
                .withMetricName(metricName)
                .withUnit(UNIT)
                .withNamespace(NAMESPACE)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withStatistics(STATISTICS_TYPE)
                .withDimensions(dimensions);
        GetMetricStatisticsResult getMetricStatisticsResult = amazonCloudWatchClient.getMetricStatisticsResult(getMetricStatisticsRequest);
        return getMetricStatisticsResult;
    }

    public GetMetricStatisticsResult getBucketSize(CloudCredential cloudCredential, String region, Date startTime, Date endTime, String bucketName) {
        Dimension bucketDimension = new Dimension().withName(BUCKET_NAME_DIMENSION).withValue(bucketName);
        Dimension storageDimension = new Dimension().withName(STORAGE_TYPE_DIMENSION).withValue(STORAGE_TYPE);
        List<Dimension> dimensionList = new ArrayList<Dimension>(Arrays.asList(bucketDimension, storageDimension));
        return getMetricsStatistics(cloudCredential, region, METRIC_NAME, startTime, endTime, dimensionList, PERIOD);
    }

}

package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.cloudwatch.model.AmazonCloudWatchException;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class AwsCloudWatchCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudWatchCommonService.class);

    private static final String BUCKET_NAME_DIMENSION = "BucketName";

    private static final String STORAGE_TYPE = "StandardStorage";

    private static final String STORAGE_TYPE_DIMENSION = "StorageType";

    private static final String NAMESPACE = "AWS/S3";

    private static final String METRIC_NAME = "BucketSizeBytes";

    private static final String STATISTICS_TYPE = "Maximum";

    private static final String UNIT = "Bytes";

    private static final int PERIOD = 3600;

    @Inject
    private CommonAwsClient awsClient;

    public GetMetricStatisticsResult getBucketSize(CloudCredential cloudCredential, String region, Date startTime, Date endTime, String bucketName) {
        Dimension bucketDimension = new Dimension().withName(BUCKET_NAME_DIMENSION).withValue(bucketName);
        Dimension storageDimension = new Dimension().withName(STORAGE_TYPE_DIMENSION).withValue(STORAGE_TYPE);
        List<Dimension> dimensionList = List.of(bucketDimension, storageDimension);
        try {
            AwsCredentialView credentialView = new AwsCredentialView(cloudCredential);
            AmazonCloudWatchClient amazonCloudWatchClient = awsClient.createCloudWatchClient(credentialView, region);
            GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                    .withPeriod(PERIOD)
                    .withMetricName(METRIC_NAME)
                    .withUnit(UNIT)
                    .withNamespace(NAMESPACE)
                    .withStartTime(startTime)
                    .withEndTime(endTime)
                    .withStatistics(STATISTICS_TYPE)
                    .withDimensions(dimensionList);
            GetMetricStatisticsResult getMetricStatisticsResult = amazonCloudWatchClient.getMetricStatisticsResult(getMetricStatisticsRequest);
            LOGGER.info("Successfully queried CloudWatch for {} for bucket {} and timeframe from {} to {}. Returned number of datapoints: {}",
                    METRIC_NAME, bucketName, startTime, endTime, getMetricStatisticsResult.getDatapoints().size());
            return getMetricStatisticsResult;
        } catch (AmazonCloudWatchException e) {
            String message = String.format("Cannot get %s for bucket %s and timeframe from %s to %s. Reason: %s",
                    METRIC_NAME, bucketName, startTime, endTime, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudConnectorException(message, e);
        }
    }
}

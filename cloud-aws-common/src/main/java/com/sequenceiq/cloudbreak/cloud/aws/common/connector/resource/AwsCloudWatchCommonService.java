package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

@Service
public class AwsCloudWatchCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudWatchCommonService.class);

    private static final String BUCKET_NAME_DIMENSION = "BucketName";

    private static final String STORAGE_TYPE = "StandardStorage";

    private static final String STORAGE_TYPE_DIMENSION = "StorageType";

    private static final String NAMESPACE = "AWS/S3";

    private static final String METRIC_NAME = "BucketSizeBytes";

    private static final String UNIT = "Bytes";

    private static final int PERIOD = 3600;

    @Inject
    private CommonAwsClient awsClient;

    public GetMetricStatisticsResponse getBucketSize(CloudCredential cloudCredential, String region, Date startTime, Date endTime, String bucketName) {
        Dimension bucketDimension = Dimension.builder().name(BUCKET_NAME_DIMENSION).value(bucketName).build();
        Dimension storageDimension = Dimension.builder().name(STORAGE_TYPE_DIMENSION).value(STORAGE_TYPE).build();
        List<Dimension> dimensionList = List.of(bucketDimension, storageDimension);
        try {
            AwsCredentialView credentialView = new AwsCredentialView(cloudCredential);
            AmazonCloudWatchClient amazonCloudWatchClient = awsClient.createCloudWatchClient(credentialView, region);
            GetMetricStatisticsRequest getMetricStatisticsRequest = GetMetricStatisticsRequest.builder()
                    .period(PERIOD)
                    .metricName(METRIC_NAME)
                    .unit(UNIT)
                    .namespace(NAMESPACE)
                    .startTime(startTime.toInstant())
                    .endTime(endTime.toInstant())
                    .statistics(Statistic.MAXIMUM)
                    .dimensions(dimensionList)
                    .build();
            GetMetricStatisticsResponse getMetricStatisticsResponse = amazonCloudWatchClient.getMetricStatisticsResponse(getMetricStatisticsRequest);
            LOGGER.info("Successfully queried CloudWatch for {} for bucket {} and timeframe from {} to {}. Returned number of datapoints: {}",
                    METRIC_NAME, bucketName, startTime, endTime, getMetricStatisticsResponse.datapoints().size());
            return getMetricStatisticsResponse;
        } catch (CloudWatchException e) {
            String message = String.format("Cannot get %s for bucket %s and timeframe from %s to %s. Reason: %s",
                    METRIC_NAME, bucketName, startTime, endTime, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudConnectorException(message, e);
        }
    }
}

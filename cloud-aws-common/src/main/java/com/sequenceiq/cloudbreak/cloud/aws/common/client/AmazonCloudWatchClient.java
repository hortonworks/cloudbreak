package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.DeleteAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DeleteAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricAlarmRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricAlarmResponse;

public class AmazonCloudWatchClient extends AmazonClient {

    private final CloudWatchClient client;

    public AmazonCloudWatchClient(CloudWatchClient client) {
        this.client = client;
    }

    public PutMetricAlarmResponse putMetricAlarm(PutMetricAlarmRequest metricAlarmRequest) {
        return client.putMetricAlarm(metricAlarmRequest);
    }

    public DescribeAlarmsResponse describeAlarms(DescribeAlarmsRequest request) {
        return client.describeAlarms(request);
    }

    public DeleteAlarmsResponse deleteAlarms(DeleteAlarmsRequest deleteAlarmsRequest) {
        return client.deleteAlarms(deleteAlarmsRequest);
    }

    public GetMetricStatisticsResponse getMetricStatisticsResponse(GetMetricStatisticsRequest getMetricStatisticsRequest) {
        return client.getMetricStatistics(getMetricStatisticsRequest);
    }
}

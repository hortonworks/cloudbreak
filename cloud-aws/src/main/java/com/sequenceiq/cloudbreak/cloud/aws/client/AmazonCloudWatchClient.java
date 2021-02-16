package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsResult;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmResult;

public class AmazonCloudWatchClient extends AmazonClient {

    private final AmazonCloudWatch client;

    public AmazonCloudWatchClient(AmazonCloudWatch client) {
        this.client = client;
    }

    public PutMetricAlarmResult putMetricAlarm(PutMetricAlarmRequest metricAlarmRequest) {
        return client.putMetricAlarm(metricAlarmRequest);
    }

    public DescribeAlarmsResult describeAlarms(DescribeAlarmsRequest request) {
        return client.describeAlarms(request);
    }

    public DeleteAlarmsResult deleteAlarms(DeleteAlarmsRequest deleteAlarmsRequest) {
        return client.deleteAlarms(deleteAlarmsRequest);
    }
}

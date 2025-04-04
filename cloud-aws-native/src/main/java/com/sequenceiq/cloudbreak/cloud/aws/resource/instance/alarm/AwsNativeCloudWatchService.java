package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.alarm;

import static java.util.stream.Collectors.toList;
import static software.amazon.awssdk.services.cloudwatch.model.Statistic.MAXIMUM;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import software.amazon.awssdk.services.cloudwatch.model.AlarmType;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.DeleteAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricAlarmRequest;

@Service
public class AwsNativeCloudWatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeCloudWatchService.class);

    private static final String EC2_NAMESPACE_VALUE = "AWS/EC2";

    private static final String GENERAL_METRIC_NAME = "StatusCheckFailed_System";

    private static final String GREATER_THAN_OR_EQUAL_TO_THRESHOLD_COMPARISON_OPERATOR = "GreaterThanOrEqualToThreshold";

    private static final String DIMENSION_NAME_FOR_INSTANCE = "InstanceId";

    private static final String ALARM_ACTION_PREFIX = "arn:%s:automate:";

    private static final String ALARM_ACTION_EC2_RECOVER_SUFFIX = ":ec2:recover";

    @Value("${aws.cloudwatch.suffix:-Status-Check-Failed-System}")
    private String alarmSuffix;

    @Value("${aws.cloudwatch.period:60}")
    private int cloudwatchPeriod;

    @Value("${aws.cloudwatch.evaluationPeriods:2}")
    private int cloudwatchEvaluationPeriods;

    @Value("${aws.cloudwatch.threshold:1.0}")
    private double cloudwatchThreshold;

    @Value("${aws.cloudwatch.max-batchsize:100}")
    private int maxBatchsize;

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private AwsTaggingService awsTaggingService;

    public void addCloudWatchAlarmsForSystemFailures(CloudResource resource, String regionName, AwsCredentialView credentialView,
            Map<String, String> userDefinedTags) {
        try {
            PutMetricAlarmRequest metricAlarmRequest = createPutMetricAlarmRequest(resource.getInstanceId(), regionName,
                    credentialView.isGovernmentCloudEnabled(), userDefinedTags);
            LOGGER.debug("The following cloudwatch alarm - for instanceId: {} - has created and about to put it on AWS side: [{}]",
                    resource.getInstanceId(), metricAlarmRequest);
            commonAwsClient.createCloudWatchClient(credentialView, regionName).putMetricAlarm(metricAlarmRequest);
        } catch (CloudWatchException acwe) {
            LOGGER.warn("Unable to create cloudwatch alarm for instanceId {}: {}", resource.getInstanceId(), acwe.getLocalizedMessage(), acwe);
        }
    }

    @Override
    public String toString() {
        return "AwsNativeCloudWatchService{" +
                "alarmSuffix='" + alarmSuffix + '\'' +
                ", cloudwatchPeriod=" + cloudwatchPeriod +
                ", cloudwatchEvaluationPeriods=" + cloudwatchEvaluationPeriods +
                ", cloudwatchThreshold=" + cloudwatchThreshold +
                ", maxBatchsize=" + maxBatchsize +
                ", commonAwsClient=" + commonAwsClient.toString() +
                '}';
    }

    public void deleteCloudWatchAlarmsForSystemFailures(String regionName, AwsCredentialView credentialView, List<String> instanceIds) {
        // The list of alarms received may be longer than 100 items, but alarms can only be deleted in batches of max
        // size 100. To work around this, break the instanceIds list into chunks no greater than 100, and process each
        // chunk. See https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_DeleteAlarms.html
        AtomicInteger counter = new AtomicInteger(0);
        Map<Integer, List<String>> grouping = instanceIds.stream()
                .map(instanceId -> instanceId + alarmSuffix)
                .collect(Collectors.groupingBy(s -> counter.getAndIncrement() / maxBatchsize));
        LOGGER.debug("Batched cloudwatch alarm delete requests: {}", grouping);
        grouping.values()
                .stream()
                .flatMap(alarmNames -> getExistingCloudWatchAlarms(regionName, credentialView, alarmNames))
                .filter(alarmNames -> !alarmNames.isEmpty())
                .forEach(alarmNames -> deleteCloudWatchAlarms(regionName, credentialView, alarmNames));
    }

    public List<MetricAlarm> getMetricAlarmsForInstances(String regionName, AwsCredentialView credentialView, List<String> instanceIds) {
        List<String> alarmNames = new ArrayList<>();
        for (String instanceId : instanceIds) {
            alarmNames.add(instanceId + alarmSuffix);
        }
        DescribeAlarmsRequest req = DescribeAlarmsRequest.builder()
                .alarmTypes(AlarmType.METRIC_ALARM)
                .alarmNames(alarmNames)
                .build();
        return commonAwsClient.createCloudWatchClient(credentialView, regionName).describeAlarms(req).metricAlarms();
    }

    private Stream<List<String>> getExistingCloudWatchAlarms(String regionName, AwsCredentialView credentialView, List<String> alarmNames) {
        Stream<List<String>> filteredAlarmNamesStream;
        LOGGER.info("Searching for cloudwatch alarms [{}]", alarmNames);
        try {
            DescribeAlarmsRequest request = DescribeAlarmsRequest.builder().alarmNames(alarmNames).maxRecords(maxBatchsize).build();
            List<String> filteredAlarmNames = commonAwsClient.createCloudWatchClient(credentialView, regionName)
                    .describeAlarms(request)
                    .metricAlarms()
                    .stream()
                    .map(MetricAlarm::alarmName)
                    .collect(toList());
            filteredAlarmNamesStream = Stream.of(filteredAlarmNames);
            LOGGER.debug("Checking cloudwatch alarms [{}] for existence and found [{}]", alarmNames, filteredAlarmNames);
        } catch (CloudWatchException acwe) {
            LOGGER.error("Unable to describe cloudwatch alarms falling back to delete all alarms individually [{}]: {}", alarmNames,
                    acwe.getLocalizedMessage(), acwe);
            filteredAlarmNamesStream = alarmNames.stream().map(List::of);
        }
        return filteredAlarmNamesStream;
    }

    private void deleteCloudWatchAlarms(String regionName, AwsCredentialView credentialView, List<String> alarmNames) {
        LOGGER.info("Attempting to delete cloudwatch alarms [{}]", alarmNames);
        try {
            DeleteAlarmsRequest deleteAlarmsRequest = DeleteAlarmsRequest.builder().alarmNames(alarmNames).build();
            commonAwsClient.createCloudWatchClient(credentialView, regionName).deleteAlarms(deleteAlarmsRequest);
            LOGGER.info("Deleted cloudwatch alarms [{}]", alarmNames);
        } catch (CloudWatchException acwe) {
            LOGGER.error("Unable to delete cloudwatch alarms [{}]: {}", alarmNames, acwe.getLocalizedMessage(), acwe);
            throw new CloudConnectorException("Unable to delete cloud watch alarms: " + acwe.getLocalizedMessage(), acwe);
        }
    }

    private PutMetricAlarmRequest createPutMetricAlarmRequest(String instanceId, String regionName, boolean govCloud, Map<String, String> userDefinedTags) {
        return PutMetricAlarmRequest.builder()
                .period(cloudwatchPeriod)
                .namespace(EC2_NAMESPACE_VALUE)
                .metricName(GENERAL_METRIC_NAME)
                .comparisonOperator(GREATER_THAN_OR_EQUAL_TO_THRESHOLD_COMPARISON_OPERATOR)
                .dimensions(createMetricAlarmDimensionWithInstanceId(instanceId))
                .alarmActions(combineAlarmActionsValue(regionName, govCloud))
                .evaluationPeriods(cloudwatchEvaluationPeriods)
                .alarmName(instanceId + alarmSuffix)
                .threshold(cloudwatchThreshold)
                .statistic(MAXIMUM)
                .tags(awsTaggingService.prepareCloudWatchTags(userDefinedTags))
                .build();
    }

    private Dimension createMetricAlarmDimensionWithInstanceId(String instanceId) {
        return Dimension.builder().name(DIMENSION_NAME_FOR_INSTANCE).value(instanceId).build();
    }

    private String combineAlarmActionsValue(String regionName, boolean govCloud) {
        String segment = govCloud ? "aws-us-gov" : "aws";
        return String.format(ALARM_ACTION_PREFIX, segment) + regionName + ALARM_ACTION_EC2_RECOVER_SUFFIX;
    }

}

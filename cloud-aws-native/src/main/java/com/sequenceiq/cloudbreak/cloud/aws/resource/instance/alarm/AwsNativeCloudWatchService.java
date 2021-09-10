package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.alarm;

import static com.amazonaws.services.cloudwatch.model.Statistic.Maximum;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.cloudwatch.model.AlarmType;
import com.amazonaws.services.cloudwatch.model.AmazonCloudWatchException;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Service
public class AwsNativeCloudWatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeCloudWatchService.class);

    private static final String EC2_NAMESPACE_VALUE = "AWS/EC2";

    private static final String GENERAL_METRIC_NAME = "StatusCheckFailed_System";

    private static final String GREATER_THAN_OR_EQUAL_TO_THRESHOLD_COMPARISON_OPERATOR = "GreaterThanOrEqualToThreshold";

    private static final String DIMENSION_NAME_FOR_INSTANCE = "InstanceId";

    private static final String ALARM_ACTION_PREFIX = "arn:aws:automate:";

    private static final String ALARM_ACTION_EC2_RECOVER_SUFFIX = ":ec2:recover";

    @Value("${aws.cloudwatch.suffix:-Status-Check-Failed-System}")
    private String alarmSuffix;

    @Value("${aws.cloudwatch.period:60}")
    private int cloudwatchPeriod;

    @Value("${aws.cloudwatch.evaluationPeriods:2}")
    private int cloudwatchEvaluationPeriods;

    @Value("${aws.cloudwatch.threshold:1.0}")
    private double cloudwatchThreshhold;

    @Value("${aws.cloudwatch.max-batchsize:100}")
    private int maxBatchsize;

    private CommonAwsClient commonAwsClient;

    public AwsNativeCloudWatchService(CommonAwsClient commonAwsClient) {
        this.commonAwsClient = commonAwsClient;
        LOGGER.info(AwsNativeCloudWatchService.class.getSimpleName() + " has created with: []");
    }

    public void addCloudWatchAlarmsForSystemFailures(CloudResource resource, String regionName, AwsCredentialView credentialView) {
        try {
            PutMetricAlarmRequest metricAlarmRequest = createPutMetricAlarmRequest(resource.getInstanceId(), regionName);
            LOGGER.debug("The following cloudwatch alarm – for instanceId: {} – has created and about to put it on AWS side: [{}]",
                    resource.getReference(), metricAlarmRequest);
            commonAwsClient.createCloudWatchClient(credentialView, regionName).putMetricAlarm(metricAlarmRequest);
        } catch (AmazonCloudWatchException acwe) {
            LOGGER.error("Unable to create cloudwatch alarm for instanceId {}: {}", resource.getReference(), acwe.getLocalizedMessage());
        }
    }

    public void deleteAllCloudWatchAlarmsForSystemFailures(CloudStack stack, String regionName, AwsCredentialView credentialView) {
        List<String> instanceIds = stack.getGroups().stream()
                .flatMap(group -> group.getInstances().stream())
                .map(CloudInstance::getInstanceId)
                .collect(toList());
        LOGGER.info("Found ids for running instances: [{}]. Cloudwatch alarms for these instances will be deleted.", instanceIds);
        List<String> deletedInstanceIds = stack.getGroups().stream()
                .flatMap(group -> group.getDeletedInstances().stream())
                .map(CloudInstance::getInstanceId)
                .collect(toList());
        LOGGER.info("Found ids for deleted instances: [{}]. Deletion will be attempted on the cloudwatch alarms for these instances.", deletedInstanceIds);
        instanceIds.addAll(deletedInstanceIds);
        deleteCloudWatchAlarmsForSystemFailures(stack, regionName, credentialView, instanceIds);
    }

    public void deleteCloudWatchAlarmsForSystemFailures(CloudStack stack, String regionName, AwsCredentialView credentialView, List<String> instanceIds) {
        if (isEmpty(instanceIds)) {
            LOGGER.warn("No instance ids provided for cloudwatch alarm deletion. No alarms will be deleted.");
        } else {
            LOGGER.info("Deleting alarms for instance ids [{}]", instanceIds.stream().collect(Collectors.joining(",")));
            List<String> instanceIdsFromStack = stack.getGroups().stream()
                    .flatMap(group -> group.getInstances().stream())
                    .map(CloudInstance::getInstanceId)
                    .collect(toList());
            List<String> instanceIdsNotInStack = instanceIds.stream()
                    .filter(instanceId -> !instanceIdsFromStack.contains(instanceId))
                    .collect(toList());
            if (!instanceIdsNotInStack.isEmpty()) {
                LOGGER.warn("Instance IDs [{}] are not part of cloud stack {}, these instances may have already been deleted on the cloud provider side.",
                        instanceIdsNotInStack, stack);
            }
            deleteCloudWatchAlarmsForSystemFailures(regionName, credentialView, instanceIds);
        }
    }

    @Override
    public String toString() {
        return "AwsNativeCloudWatchService{" +
                "alarmSuffix='" + alarmSuffix + '\'' +
                ", cloudwatchPeriod=" + cloudwatchPeriod +
                ", cloudwatchEvaluationPeriods=" + cloudwatchEvaluationPeriods +
                ", cloudwatchThreshhold=" + cloudwatchThreshhold +
                ", maxBatchsize=" + maxBatchsize +
                ", commonAwsClient=" + commonAwsClient.toString() +
                '}';
    }

    public void deleteCloudWatchAlarmsForSystemFailures(String regionName, AwsCredentialView credentialView, List<String> instanceIds) {
        // The list of alarms received may be longer than 100 items, but alarms can only be deleted in batches of max
        // size 100. To work around this, break the instanceIds list into chunks no greater than 100, and process each
        // chunk. See https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_DeleteAlarms.html
        final AtomicInteger counter = new AtomicInteger(0);
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

    public List<MetricAlarm> getMetricAlarmsForInstances(String regionName, AwsCredentialView credentialView, Set<String> instanceIds) {
        List<String> alarmNames = new ArrayList<>();
        for (String instanceId : instanceIds) {
            alarmNames.add(instanceId + alarmSuffix);
        }
        DescribeAlarmsRequest req = new DescribeAlarmsRequest()
                .withAlarmTypes(AlarmType.MetricAlarm)
                .withAlarmNames(alarmNames);
        return commonAwsClient.createCloudWatchClient(credentialView, regionName).describeAlarms(req).getMetricAlarms();
    }

    private Stream<List<String>> getExistingCloudWatchAlarms(String regionName, AwsCredentialView credentialView, List<String> alarmNames) {
        Stream<List<String>> filteredAlarmNamesStream;
        LOGGER.info("Searching for cloudwatch alarms [{}]", alarmNames);
        try {
            DescribeAlarmsRequest request = new DescribeAlarmsRequest().withAlarmNames(alarmNames).withMaxRecords(maxBatchsize);
            List<String> filteredAlarmNames = commonAwsClient.createCloudWatchClient(credentialView, regionName)
                    .describeAlarms(request)
                    .getMetricAlarms()
                    .stream()
                    .map(MetricAlarm::getAlarmName)
                    .collect(toList());
            filteredAlarmNamesStream = Stream.of(filteredAlarmNames);
            LOGGER.debug("Checking cloudwatch alarms [{}] for existence and found [{}]", alarmNames, filteredAlarmNames);
        } catch (AmazonCloudWatchException acwe) {
            LOGGER.error("Unable to describe cloudwatch alarms falling back to delete all alarms individually [{}]: {}", alarmNames,
                    acwe.getLocalizedMessage());
            filteredAlarmNamesStream = alarmNames.stream().map(alarmName -> List.of(alarmName));
        }
        return filteredAlarmNamesStream;
    }

    private void deleteCloudWatchAlarms(String regionName, AwsCredentialView credentialView, List<String> alarmNames) {
        LOGGER.info("Attempting to delete cloudwatch alarms [{}]", alarmNames);
        try {
            DeleteAlarmsRequest deleteAlarmsRequest = new DeleteAlarmsRequest().withAlarmNames(alarmNames);
            commonAwsClient.createCloudWatchClient(credentialView, regionName).deleteAlarms(deleteAlarmsRequest);
            LOGGER.info("Deleted cloudwatch alarms [{}]", alarmNames);
        } catch (AmazonCloudWatchException acwe) {
            LOGGER.error("Unable to delete cloudwatch alarms [{}]: {}", alarmNames, acwe.getLocalizedMessage());
            throw new CloudConnectorException("Unable to delete cloud watch alarms: " + acwe.getLocalizedMessage(), acwe);
        }
    }

    private PutMetricAlarmRequest createPutMetricAlarmRequest(String instanceId, String regionName) {
        return new PutMetricAlarmRequest()
                .withPeriod(cloudwatchPeriod)
                .withNamespace(EC2_NAMESPACE_VALUE)
                .withMetricName(GENERAL_METRIC_NAME)
                .withComparisonOperator(GREATER_THAN_OR_EQUAL_TO_THRESHOLD_COMPARISON_OPERATOR)
                .withDimensions(createMetricAlarmDimensionWithInstanceId(instanceId))
                .withAlarmActions(combineAlarmActionsValue(regionName))
                .withEvaluationPeriods(cloudwatchEvaluationPeriods)
                .withAlarmName(instanceId + alarmSuffix)
                .withThreshold(cloudwatchThreshhold)
                .withStatistic(Maximum);
    }

    private Dimension createMetricAlarmDimensionWithInstanceId(String instanceId) {
        return new Dimension().withName(DIMENSION_NAME_FOR_INSTANCE).withValue(instanceId);
    }

    private String combineAlarmActionsValue(String regionName) {
        return ALARM_ACTION_PREFIX + regionName + ALARM_ACTION_EC2_RECOVER_SUFFIX;
    }

}

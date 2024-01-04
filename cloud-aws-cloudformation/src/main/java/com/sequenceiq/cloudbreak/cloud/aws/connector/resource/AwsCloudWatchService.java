package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.INSTANCE_TYPE;

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

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.DeleteAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricAlarmRequest;

@Service
public class AwsCloudWatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudWatchService.class);

    private static final List<String> RECOVERABLE_INSTANCE_TYPES = List.of("a1", "c3", "c4", "c5", "c5a", "c5n", "c6g", "c6gn", "inf1", "m3", "m4", "m5", "m5a",
            "m5n", "m5zn", "m6g", "m6i", "p3", "p4", "r3", "r4", "r5", "r5a", "r5b", "r5n", "r6g", "t2", "t3", "t3a", "t4g", "x1", "x1e");

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

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private AwsTaggingService awsTaggingService;

    public void addCloudWatchAlarmsForSystemFailures(List<CloudResource> instances, String regionName, AwsCredentialView credentialView,
            Map<String, String> userDefinedTags) {
        AmazonCloudWatchClient amazonCloudWatchClient = awsClient.createCloudWatchClient(credentialView, regionName);

        instances.stream().filter(instance -> {
            String instanceType = instance.getStringParameter(INSTANCE_TYPE);
            if (instanceType == null) {
                LOGGER.debug("Cannot determine if recovery is supported by instance type, attempting to set it up.");
                return true;
            }
            String family = instanceType.contains(".") ? instanceType.split("\\.")[0] : instanceType;
            return RECOVERABLE_INSTANCE_TYPES.contains(family);
        }).forEach(instance -> {
            try {
                String segment = credentialView.isGovernmentCloudEnabled() ? "aws-us-gov" : "aws";
                PutMetricAlarmRequest metricAlarmRequest = PutMetricAlarmRequest.builder()
                        .alarmActions(List.of("arn:" + segment + ":automate:" + regionName + ":ec2:recover"))
                        .alarmName(instance.getInstanceId() + alarmSuffix)
                        .metricName("StatusCheckFailed_System")
                        .statistic("Maximum")
                        .namespace("AWS/EC2")
                        .dimensions(List.of(Dimension.builder().name("InstanceId").value(instance.getInstanceId()).build()))
                        .period(cloudwatchPeriod)
                        .evaluationPeriods(cloudwatchEvaluationPeriods)
                        .threshold(cloudwatchThreshhold)
                        .comparisonOperator("GreaterThanOrEqualToThreshold")
                        .tags(awsTaggingService.prepareCloudWatchTags(userDefinedTags))
                        .build();
                amazonCloudWatchClient.putMetricAlarm(metricAlarmRequest);
                LOGGER.debug("Created cloudwatch alarm for instanceId {}.", instance.getInstanceId());
            } catch (CloudWatchException acwe) {
                LOGGER.info("Unable to create cloudwatch alarm for instanceId {} (instance type: {}): {}", instance.getInstanceId(),
                        instance.getStringParameter(INSTANCE_TYPE), acwe.getLocalizedMessage());
            }
        });
    }

    public void deleteAllCloudWatchAlarmsForSystemFailures(CloudStack stack, String regionName, AwsCredentialView credentialView) {
        List<String> instanceIds = stack.getGroups().stream()
                .flatMap(group -> group.getInstances().stream())
                .map(CloudInstance::getInstanceId)
                .collect(Collectors.toList());
        LOGGER.info("Found ids for running instances: [{}]. Cloudwatch alarms for these instances will be deleted.", instanceIds);
        List<String> deletedInstanceIds = stack.getGroups().stream()
                .flatMap(group -> group.getDeletedInstances().stream())
                .map(CloudInstance::getInstanceId)
                .collect(Collectors.toList());
        LOGGER.info("Found ids for deleted instances: [{}]. Deletion will be attempted on the cloudwatch alarms for these instances.", deletedInstanceIds);
        instanceIds.addAll(deletedInstanceIds);
        deleteCloudWatchAlarmsForSystemFailures(stack, regionName, credentialView, instanceIds);
    }

    public void deleteCloudWatchAlarmsForSystemFailures(CloudStack stack, String regionName, AwsCredentialView credentialView, List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            LOGGER.warn("No instance ids provided for cloudwatch alarm deletion. No alarms will be deleted.");
        } else {
            LOGGER.info("Deleting alarms for instance ids [{}]", instanceIds);
            List<String> instanceIdsFromStack = stack.getGroups().stream()
                    .flatMap(group -> group.getInstances().stream())
                    .map(CloudInstance::getInstanceId)
                    .collect(Collectors.toList());
            List<String> instanceIdsNotInStack = instanceIds.stream()
                    .filter(instanceId -> !instanceIdsFromStack.contains(instanceId))
                    .collect(Collectors.toList());
            if (!instanceIdsNotInStack.isEmpty()) {
                LOGGER.warn("Instance IDs [{}] are not part of cloud stack {}, these instances may have already been deleted on the cloud provider side.",
                        instanceIdsNotInStack, stack);
            }
            deleteCloudWatchAlarmsForSystemFailures(regionName, credentialView, instanceIds);
        }
    }

    private void deleteCloudWatchAlarmsForSystemFailures(String regionName, AwsCredentialView credentialView, List<String> instanceIds) {
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

    private Stream<List<String>> getExistingCloudWatchAlarms(String regionName, AwsCredentialView credentialView, List<String> alarmNames) {
        Stream<List<String>> filteredAlarmNamesStream;
        AmazonCloudWatchClient amazonCloudWatchClient = awsClient.createCloudWatchClient(credentialView, regionName);

        LOGGER.info("Searching for cloudwatch alarms [{}]", alarmNames);
        try {
            DescribeAlarmsRequest request = DescribeAlarmsRequest.builder().alarmNames(alarmNames).maxRecords(maxBatchsize).build();
            List<String> filteredAlarmNames = amazonCloudWatchClient.describeAlarms(request).metricAlarms().stream()
                    .map(MetricAlarm::alarmName)
                    .collect(Collectors.toList());
            filteredAlarmNamesStream = Stream.of(filteredAlarmNames);
            LOGGER.debug("Checking cloudwatch alarms [{}] for existence and found [{}]", alarmNames, filteredAlarmNames);
        } catch (CloudWatchException acwe) {
            LOGGER.error("Unable to describe cloudwatch alarms falling back to delete all alarms individually [{}]: {}", alarmNames, acwe.getLocalizedMessage());
            filteredAlarmNamesStream = alarmNames.stream()
                    .map(List::of);
        }
        return filteredAlarmNamesStream;
    }

    private void deleteCloudWatchAlarms(String regionName, AwsCredentialView credentialView, List<String> alarmNames) {
        LOGGER.info("Attempting to delete cloudwatch alarms [{}]", alarmNames);
        try {
            DeleteAlarmsRequest deleteAlarmsRequest = DeleteAlarmsRequest.builder().alarmNames(alarmNames).build();
            AmazonCloudWatchClient amazonCloudWatchClient = awsClient.createCloudWatchClient(credentialView, regionName);
            amazonCloudWatchClient.deleteAlarms(deleteAlarmsRequest);
            LOGGER.info("Deleted cloudwatch alarms [{}]", alarmNames);
        } catch (CloudWatchException acwe) {
            LOGGER.error("Unable to delete cloudwatch alarms [{}]: {}", alarmNames, acwe.getLocalizedMessage());
            throw new CloudConnectorException("Unable to delete cloud watch alarms: " + acwe.getLocalizedMessage(), acwe);
        }
    }
}

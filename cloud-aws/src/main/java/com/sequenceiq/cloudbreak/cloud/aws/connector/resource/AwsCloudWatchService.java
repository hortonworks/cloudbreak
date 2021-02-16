package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.cloudwatch.model.AmazonCloudWatchException;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Service
public class AwsCloudWatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudWatchService.class);

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
    private AwsClient awsClient;

    public void addCloudWatchAlarmsForSystemFailures(List<CloudResource> instances, String regionName, AwsCredentialView credentialView) {
        AmazonCloudWatchClient amazonCloudWatchClient = awsClient.createCloudWatchClient(credentialView, regionName);

        instances.stream().forEach(instance -> {
            try {
                PutMetricAlarmRequest metricAlarmRequest = new PutMetricAlarmRequest();
                metricAlarmRequest.setAlarmActions(Arrays.asList("arn:aws:automate:" + regionName + ":ec2:recover"));
                metricAlarmRequest.setAlarmName(instance.getInstanceId() + alarmSuffix);
                metricAlarmRequest.setMetricName("StatusCheckFailed_System");
                metricAlarmRequest.setStatistic("Maximum");
                metricAlarmRequest.setNamespace("AWS/EC2");
                metricAlarmRequest.setDimensions(Arrays.asList(new Dimension().withName("InstanceId").withValue(instance.getInstanceId())));
                metricAlarmRequest.setPeriod(cloudwatchPeriod);
                metricAlarmRequest.setEvaluationPeriods(cloudwatchEvaluationPeriods);
                metricAlarmRequest.setThreshold(cloudwatchThreshhold);
                metricAlarmRequest.setComparisonOperator("GreaterThanOrEqualToThreshold");
                amazonCloudWatchClient.putMetricAlarm(metricAlarmRequest);
                LOGGER.debug("Created cloudwatch alarm for instanceId {}.", instance.getInstanceId());
            } catch (AmazonCloudWatchException acwe) {
                LOGGER.error("Unable to create cloudwatch alarm for instanceId {}: {}", instance.getInstanceId(), acwe.getLocalizedMessage());
            }
        });
    }

    public void deleteCloudWatchAlarmsForSystemFailures(CloudStack stack, String regionName, AwsCredentialView credentialView) {
        List<String> instanceIds = stack.getGroups().stream()
                .flatMap(group -> group.getInstances().stream())
                .map(CloudInstance::getInstanceId)
                .collect(Collectors.toList());
        deleteCloudWatchAlarmsForSystemFailures(stack, regionName, credentialView, instanceIds);
    }

    public void deleteCloudWatchAlarmsForSystemFailures(CloudStack stack, String regionName, AwsCredentialView credentialView, List<String> instanceIds) {
        List<String> instanceIdsFromStack = stack.getGroups().stream()
                .flatMap(group -> group.getInstances().stream())
                .map(CloudInstance::getInstanceId)
                .collect(Collectors.toList());
        List<String> instanceIdsNotInStack = instanceIds.stream()
                .filter(instanceId -> !instanceIdsFromStack.contains(instanceId))
                .collect(Collectors.toList());
        if (!instanceIdsNotInStack.isEmpty()) {
            LOGGER.warn("Instance IDs [{}] are not part of cloud stack {}, these instances may have already been deleted on the cloud provider side.",
                    instanceIdsFromStack, stack);
        }
        deleteCloudWatchAlarmsForSystemFailures(regionName, credentialView, instanceIds);
    }

    private void deleteCloudWatchAlarmsForSystemFailures(String regionName, AwsCredentialView credentialView, List<String> instanceIds) {
        final AtomicInteger counter = new AtomicInteger(0);
        instanceIds.stream()
                .map(instanceId -> instanceId + alarmSuffix)
                .collect(Collectors.groupingBy(s -> counter.getAndIncrement() / maxBatchsize))
                .values()
                .stream()
                .flatMap(alarmNames -> getExistingCloudWatchAlarms(regionName, credentialView, alarmNames))
                .filter(alarmNames -> !alarmNames.isEmpty())
                .forEach(alarmNames -> deleteCloudWatchAlarms(regionName, credentialView, alarmNames));
    }

    private Stream<List<String>> getExistingCloudWatchAlarms(String regionName, AwsCredentialView credentialView, List<String> alarmNames) {
        Stream<List<String>> filteredAlarmNamesStream;
        AmazonCloudWatchClient amazonCloudWatchClient = awsClient.createCloudWatchClient(credentialView, regionName);

        try {
            DescribeAlarmsRequest request = new DescribeAlarmsRequest().withAlarmNames(alarmNames).withMaxRecords(maxBatchsize);
            List<String> filteredAlarmNames = amazonCloudWatchClient.describeAlarms(request).getMetricAlarms().stream()
                    .map(MetricAlarm::getAlarmName)
                    .collect(Collectors.toList());
            filteredAlarmNamesStream = Stream.of(filteredAlarmNames);
            LOGGER.debug("Checking cloudwatch alarms [{}] for existence and found [{}]", alarmNames, filteredAlarmNames);
        } catch (AmazonCloudWatchException acwe) {
            LOGGER.error("Unable to describe cloudwatch alarms falling back to delete all alarms individually [{}]: {}", alarmNames, acwe.getLocalizedMessage());
            filteredAlarmNamesStream = alarmNames.stream()
                    .map(alarmName -> List.of(alarmName));
        }
        return filteredAlarmNamesStream;
    }

    private void deleteCloudWatchAlarms(String regionName, AwsCredentialView credentialView, List<String> alarmNames) {
        try {
            DeleteAlarmsRequest deleteAlarmsRequest = new DeleteAlarmsRequest().withAlarmNames(alarmNames);
            AmazonCloudWatchClient amazonCloudWatchClient = awsClient.createCloudWatchClient(credentialView, regionName);
            amazonCloudWatchClient.deleteAlarms(deleteAlarmsRequest);
            LOGGER.debug("Deleted cloudwatch alarms [{}]", alarmNames);
        } catch (AmazonCloudWatchException acwe) {
            LOGGER.error("Unable to delete cloudwatch alarms [{}]: {}", alarmNames, acwe.getLocalizedMessage());
            throw new CloudConnectorException("unable to delete cloud watch alarms", acwe);
        }
    }
}

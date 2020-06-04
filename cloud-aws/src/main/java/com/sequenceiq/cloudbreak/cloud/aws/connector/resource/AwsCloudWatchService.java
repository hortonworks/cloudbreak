package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.CLOUDWATCH_CREATE_PARAMETER;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.AmazonCloudWatchException;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Service
public class AwsCloudWatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudWatchService.class);

    @Value("${freeipa.aws.cloudwatch.suffix:-Status-Check-Failed-System}")
    private String alarmSuffix;

    @Value("${freeipa.aws.cloudwatch.period:60}")
    private int cloudwatchPeriod;

    @Value("${freeipa.aws.cloudwatch.evaluationPeriods:2}")
    private int cloudwatchEvaluationPeriods;

    @Value("${freeipa.aws.cloudwatch.threshold:1.0}")
    private double cloudwatchThreshhold;

    @Inject
    private AwsClient awsClient;

    public void addCloudWatchAlarmsForSystemFailures(List<CloudResource> instances, CloudStack stack, String regionName, AwsCredentialView credentialView) {
        if (isCloudwatchEnabled(stack)) {
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
                    AmazonCloudWatchClient amazonCloudWatchClient = awsClient.createCloudWatchClient(credentialView, regionName);
                    amazonCloudWatchClient.putMetricAlarm(metricAlarmRequest);
                    LOGGER.debug("Created cloudwatch alarm for instanceId {}.", instance.getInstanceId());
                } catch (AmazonCloudWatchException acwe) {
                    LOGGER.error("Unable to create cloudwatch alarm for instanceId {}: {}", instance.getInstanceId(), acwe.getLocalizedMessage());
                }
            });
        }
    }

    public void deleteCloudWatchAlarmsForSystemFailures(CloudStack stack, String regionName, AwsCredentialView credentialView) {
        List<String> instanceIds = stack.getGroups().stream()
                .flatMap(group -> group.getInstances().stream())
                .map(CloudInstance::getInstanceId)
                .collect(Collectors.toList());
        deleteCloudWatchAlarmsForSystemFailures(stack, regionName, credentialView, instanceIds);
    }

    public void deleteCloudWatchAlarmsForSystemFailures(CloudStack stack, String regionName, AwsCredentialView credentialView, List<String> instanceIds) {
        if (isCloudwatchEnabled(stack)) {
            List<String> instanceIdsFromStack = stack.getGroups().stream()
                    .flatMap(group -> group.getInstances().stream())
                    .map(CloudInstance::getInstanceId)
                    .collect(Collectors.toList());
            List<String> instanceIdsNotInStack = instanceIds.stream()
                    .filter(instanceId -> !instanceIdsFromStack.contains(instanceId))
                    .collect(Collectors.toList());
            if (!instanceIdsNotInStack.isEmpty()) {
                LOGGER.error("Instance IDs [{}] are not part of stack {}", instanceIdsFromStack, stack);
                throw new CloudConnectorException("Unable to delete cloud watch alarms for system failure because instances ID are not a part of the stack.");
            }
            deleteCloudWatchAlarmsForSystemFailures(regionName, credentialView, instanceIds);
        }
    }

    private void deleteCloudWatchAlarmsForSystemFailures(String regionName, AwsCredentialView credentialView, List<String> instanceIds) {
        instanceIds.stream().forEach(instanceId -> {
            try {
                DeleteAlarmsRequest deleteAlarmsRequest = new DeleteAlarmsRequest().withAlarmNames(instanceId + alarmSuffix);
                AmazonCloudWatchClient amazonCloudWatchClient = awsClient.createCloudWatchClient(credentialView, regionName);
                amazonCloudWatchClient.deleteAlarms(deleteAlarmsRequest);
                LOGGER.debug("Deleted cloudwatch alarm for instanceId {}", instanceId);
            } catch (AmazonCloudWatchException acwe) {
                LOGGER.error("Unable to delete cloudwatch alarm for instanceId {}: {}", instanceId, acwe.getLocalizedMessage());
            }
        });
    }

    private boolean isCloudwatchEnabled(CloudStack stack) {
        String isCreate = stack.getParameters().get(CLOUDWATCH_CREATE_PARAMETER);
        return Boolean.TRUE.toString().equals(isCreate);
    }
}

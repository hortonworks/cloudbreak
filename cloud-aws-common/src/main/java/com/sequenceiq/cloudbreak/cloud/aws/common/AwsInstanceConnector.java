package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INSTANCE_NOT_FOUND;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INSUFFICIENT_INSTANCE_CAPACITY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.poller.PollerUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsInstanceStatusMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.exception.InsufficientCapacityException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.GetConsoleOutputRequest;
import software.amazon.awssdk.services.ec2.model.GetConsoleOutputResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;

@Service
public class AwsInstanceConnector implements InstanceConnector {

    public static final Integer RESILIENT_START_THRESHOLD = 100;

    private static final Integer RESILIENT_START_CHUNK_SIZE = 5;

    private static final Integer RESILIENT_START_SMALL_GROUP_THRESHOLD = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceConnector.class);

    @Inject
    private PollerUtil pollerUtil;

    @Value("${cb.aws.hostkey.verify:}")
    private boolean verifyHostKey;

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        if (!verifyHostKey) {
            throw new CloudOperationNotSupportedException("Host key verification is disabled on AWS");
        }
        AmazonEc2Client amazonEC2Client = new AuthenticatedContextView(authenticatedContext).getAmazonEC2Client();
        GetConsoleOutputRequest getConsoleOutputRequest = GetConsoleOutputRequest.builder().instanceId(vm.getInstanceId()).build();
        GetConsoleOutputResponse getConsoleOutputResponse = amazonEC2Client.getConsoleOutput(getConsoleOutputRequest);
        try {
            return getConsoleOutputResponse.output() == null ? "" : Base64Util.decode(getConsoleOutputResponse.output());
        } catch (Exception ex) {
            LOGGER.warn(ex.getMessage(), ex);
            return "";
        }
    }

    @Retryable(
            retryFor = SdkClientException.class,
            maxAttemptsExpression = "#{${cb.vm.retry.attempt:15}}",
            backoff = @Backoff(delayExpression = "#{${cb.vm.retry.backoff.delay:1000}}",
                    multiplierExpression = "#{${cb.vm.retry.backoff.multiplier:2}}",
                    maxDelayExpression = "#{${cb.vm.retry.backoff.maxdelay:10000}}")
    )
    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        String exceptionText = "Failed to send start request to AWS: ";
        String completedStatus = "Running";
        BiConsumer<AmazonEc2Client, Collection<String>> startConsumer =
                (ec2Client, instances) -> ec2Client.startInstances(StartInstancesRequest.builder().instanceIds(instances).build());
        if (vms.size() < RESILIENT_START_THRESHOLD) {
            LOGGER.debug("Sending start request for VM(s) with the following ID(s): {}",
                    vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.joining(",")));
            return setCloudVmInstanceStatuses(ac, vms, completedStatus, startConsumer, exceptionText);
        } else {
            try {
                return resilientStart(ac, resources, vms, exceptionText, completedStatus, startConsumer);
            } catch (SdkClientException e) {
                // catch and convert SdkClientException to eliminate spring retry in case of resilient start, since ec2client already has its own retry logic
                throw new CloudConnectorException(e);
            }
        }
    }

    public List<CloudVmInstanceStatus> resilientStart(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms,
            String exceptionText, String completedStatus, BiConsumer<AmazonEc2Client, Collection<String>> startConsumer) {
        List<CloudVmInstanceStatus> instanceStatuses = new ArrayList<>();
        Map<String, List<CloudInstance>> instancesByGroup = vms.stream().collect(
                Collectors.groupingBy(instance -> instance.getTemplate().getGroupName()));
        Map<List<CloudInstance>, AwsResilientStartFailureType> instanceChunksWithFailureTypeMap = getResilientStartVmChunkMap(instancesByGroup);
        instanceChunksWithFailureTypeMap.forEach((vmListChunk, failureType) ->
                executeStartForVmListChunk(ac, exceptionText, completedStatus, startConsumer, vmListChunk, failureType, instanceStatuses));
        return instanceStatuses;
    }

    private void executeStartForVmListChunk(AuthenticatedContext ac, String exceptionText, String completedStatus,
            BiConsumer<AmazonEc2Client, Collection<String>> startConsumer, List<CloudInstance> vmListChunk, AwsResilientStartFailureType failureType,
            List<CloudVmInstanceStatus> instanceStatuses) {
        String vmListString = vmListChunk.stream().map(CloudInstance::getInstanceId).collect(Collectors.joining(","));
        LOGGER.info("Resiliently starting instances: {}", vmListString);
        AmazonEc2Client amazonEC2Client = new AuthenticatedContextView(ac).getStartInstancesAmazonEC2Client();
        Set<InstanceStatus> finalStatuses = Set.of(AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(completedStatus), InstanceStatus.FAILED);
        try {
            executeInstanceOperation(ac, vmListChunk, completedStatus, startConsumer, finalStatuses, exceptionText, amazonEC2Client);
            instanceStatuses.addAll(pollerUtil.waitFor(ac, vmListChunk, finalStatuses, completedStatus));
        } catch (Exception e) {
            if (AwsResilientStartFailureType.SOFT_FAILURE.equals(failureType)) {
                LOGGER.error("Error happened during batch start of instances {}, but the failure is not blocking, moving on. Reason: {}",
                        vmListString, e.getMessage());
            } else {
                throw e;
            }
        }
    }

    private Map<List<CloudInstance>, AwsResilientStartFailureType> getResilientStartVmChunkMap(Map<String, List<CloudInstance>> instancesByGroup) {
        Map<List<CloudInstance>, AwsResilientStartFailureType> instanceChunksWithFailureTypeMap = new HashMap<>();
        instancesByGroup.values().forEach(vmsByGroup -> {
            if (vmsByGroup.size() <= RESILIENT_START_SMALL_GROUP_THRESHOLD) {
                instanceChunksWithFailureTypeMap.put(Collections.unmodifiableList(vmsByGroup), AwsResilientStartFailureType.HARD_FAILURE);
            }
        });
        instancesByGroup.values().forEach(vmsByGroup -> {
            if (vmsByGroup.size() > RESILIENT_START_SMALL_GROUP_THRESHOLD) {
                Lists.partition(vmsByGroup, RESILIENT_START_CHUNK_SIZE).forEach(vmsChunk ->
                        instanceChunksWithFailureTypeMap.put(Collections.unmodifiableList(vmsChunk), AwsResilientStartFailureType.SOFT_FAILURE));
            }
        });
        return instanceChunksWithFailureTypeMap;
    }

    @Retryable(
            value = SdkClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delayExpression = "#{${cb.vm.retry.backoff.delay:1000}}",
                    multiplierExpression = "#{${cb.vm.retry.backoff.multiplier:2}}",
                    maxDelayExpression = "#{${cb.vm.retry.backoff.maxdelay:10000}}")
    )
    @Override
    public List<CloudVmInstanceStatus> startWithLimitedRetry(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms,
            Long timeboundInMs) {
        LOGGER.debug("Sending start request for VM(s) using limited retry with the following ID(s): {}",
                vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.joining(",")));
        // The following states will never transition over to "Running"/STARTED - so it is safe to ignore instances in this state.
        Set<InstanceStatus> completedStatuses = EnumSet.of(
                InstanceStatus.FAILED,
                InstanceStatus.TERMINATED,
                InstanceStatus.TERMINATED_BY_PROVIDER,
                InstanceStatus.DELETE_REQUESTED,
                InstanceStatus.SHUTTING_DOWN,
                AwsInstanceStatusMapper.getInstanceStatusByAwsStatus("Running"));
        return setCloudVmInstanceStatuses(ac, vms, "Running",
                (ec2Client, instances) -> ec2Client.startInstances(StartInstancesRequest.builder().instanceIds(instances).build()),
                completedStatuses,
                "Failed to send start request to AWS: ", timeboundInMs);
    }

    @Retryable(
            value = SdkClientException.class,
            maxAttemptsExpression = "#{${cb.vm.retry.attempt:15}}",
            backoff = @Backoff(delayExpression = "#{${cb.vm.retry.backoff.delay:1000}}",
                    multiplierExpression = "#{${cb.vm.retry.backoff.multiplier:2}}",
                    maxDelayExpression = "#{${cb.vm.retry.backoff.maxdelay:10000}}")
    )
    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        return setCloudVmInstanceStatuses(ac, vms, "Stopped",
                (ec2Client, instances) -> ec2Client.stopInstances(StopInstancesRequest.builder().instanceIds(instances).build()),
                "Failed to send stop request to AWS: ");
    }

    @Retryable(
            value = SdkClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delayExpression = "#{${cb.vm.retry.backoff.delay:1000}}",
                    multiplierExpression = "#{${cb.vm.retry.backoff.multiplier:2}}",
                    maxDelayExpression = "#{${cb.vm.retry.backoff.maxdelay:10000}}")
    )
    @Override
    public List<CloudVmInstanceStatus> stopWithLimitedRetry(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms,
            Long timeboundInMs) {
        Set<InstanceStatus> completedStatuses = EnumSet.of(
                InstanceStatus.FAILED,
                InstanceStatus.TERMINATED,
                InstanceStatus.TERMINATED_BY_PROVIDER,
                InstanceStatus.DELETE_REQUESTED,
                InstanceStatus.SHUTTING_DOWN,
                AwsInstanceStatusMapper.getInstanceStatusByAwsStatus("Stopped"));
        return setCloudVmInstanceStatuses(ac, vms, "Stopped",
                (ec2Client, instances) -> ec2Client.stopInstances(StopInstancesRequest.builder().instanceIds(instances).build()),
                completedStatuses,
                "Failed to send stop request to AWS: ", timeboundInMs);
    }

    private List<CloudVmInstanceStatus> setCloudVmInstanceStatuses(AuthenticatedContext ac, List<CloudInstance> vms, String status,
            BiConsumer<AmazonEc2Client, Collection<String>> consumer, Set<InstanceStatus> completedStatuses, String exceptionText,
            Long timeboundInMs) {
        AmazonEc2Client amazonEC2Client = new AuthenticatedContextView(ac).getAmazonEC2Client();
        executeInstanceOperation(ac, vms, status, consumer, completedStatuses, exceptionText, amazonEC2Client);
        if (timeboundInMs != null) {
            return pollerUtil.timeBoundWaitFor(timeboundInMs, ac, vms, completedStatuses, status);
        } else {
            return pollerUtil.waitFor(ac, vms, completedStatuses, status);
        }
    }

    private void executeInstanceOperation(AuthenticatedContext ac, List<CloudInstance> vms, String status, BiConsumer<AmazonEc2Client,
            Collection<String>> consumer, Set<InstanceStatus> completedStatuses, String exceptionText, AmazonEc2Client amazonEC2Client) {
        try {
            Collection<String> instances = instanceIdsWhichAreNotInCorrectState(vms, amazonEC2Client, status, completedStatuses);
            if (!instances.isEmpty()) {
                consumer.accept(amazonEC2Client, instances);
            }
        } catch (Ec2Exception e) {
            handleEC2Exception(vms, e);
        } catch (SdkClientException e) {
            LOGGER.warn(exceptionText, e);
            throw e;
        }
    }

    private List<CloudVmInstanceStatus> setCloudVmInstanceStatuses(AuthenticatedContext ac, List<CloudInstance> vms, String status,
            BiConsumer<AmazonEc2Client, Collection<String>> consumer, String exceptionText) {
        return setCloudVmInstanceStatuses(ac, vms, status, consumer,
                Set.of(AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(status), InstanceStatus.FAILED), exceptionText, null);
    }

    @Retryable(
            value = SdkClientException.class,
            maxAttempts = 15,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    @Override
    public List<CloudVmInstanceStatus> reboot(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> rebootedVmsStatus = new ArrayList<>();

        try {
            if (!vms.isEmpty()) {
                List<CloudVmInstanceStatus> statuses = check(ac, vms);
                List<CloudInstance> startedInstances = getStarted(statuses);
                LOGGER.info("Already Stopped Vms on cloudProvider: {}", getNotStarted(statuses));
                if (!startedInstances.isEmpty()) {
                    stop(ac, null, startedInstances);
                    statuses = check(ac, vms);
                }
                logInvalidStatuses(getNotStopped(statuses), InstanceStatus.STOPPED);
                List<CloudInstance> stoppedInstances = getStopped(statuses);
                LOGGER.info("Already Started Vms on cloudProvider: {}", getNotStopped(statuses));
                if (!stoppedInstances.isEmpty()) {
                    rebootedVmsStatus = start(ac, null, stoppedInstances);
                }
                logInvalidStatuses(getNotStarted(statuses), InstanceStatus.STARTED);
            }
        } catch (SdkClientException e) {
            LOGGER.warn("Failed to send reboot request to AWS: ", e);
            throw e;
        }
        return rebootedVmsStatus;
    }

    @Retryable(
            value = SdkClientException.class,
            maxAttempts = 15,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    @Override
    public List<CloudVmInstanceStatus> restartWithLimitedRetry(AuthenticatedContext ac, List<CloudResource> resources,
            List<CloudInstance> vms, Long timeboundInMs, List<InstanceStatus> excludedStatuses) {
        LOGGER.info("Restarting vms on Aws: {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        List<CloudVmInstanceStatus> rebootedVmsStatus = new ArrayList<>();
        try {
            if (!vms.isEmpty()) {
                List<CloudVmInstanceStatus> statuses = check(ac, vms);
                List<CloudInstance> notExcludedStartedInstances = getNotExcludedStatusInstances(statuses, excludedStatuses);
                LOGGER.info("Vms of excluded statuses on cloudProvider: {}", getExcludedStatusInstances(statuses, excludedStatuses));
                if (!notExcludedStartedInstances.isEmpty()) {
                    stopWithLimitedRetry(ac, null, notExcludedStartedInstances, timeboundInMs);
                    statuses = check(ac, vms);
                }
                logInvalidStatuses(getNotStopped(statuses), InstanceStatus.STOPPED);
                List<CloudInstance> stoppedInstances = getStopped(statuses);
                LOGGER.info("Already Started Vms on cloudProvider: {}", getNotStopped(statuses));
                if (!stoppedInstances.isEmpty()) {
                    rebootedVmsStatus = startWithLimitedRetry(ac, null, stoppedInstances, timeboundInMs);
                }
                logInvalidStatuses(getNotStarted(statuses), InstanceStatus.STARTED);
            }
        } catch (SdkClientException e) {
            LOGGER.warn("Failed to send restart request to AWS: ", e);
            throw e;
        }
        return rebootedVmsStatus;
    }

    private List<CloudVmInstanceStatus> getNotStarted(List<CloudVmInstanceStatus> statuses) {
        return statuses.stream().filter(status -> status.getStatus() != InstanceStatus.STARTED)
                .collect(Collectors.toList());
    }

    private List<CloudVmInstanceStatus> getNotStopped(List<CloudVmInstanceStatus> statuses) {
        return statuses.stream().filter(status -> status.getStatus() != InstanceStatus.STOPPED)
                .collect(Collectors.toList());
    }

    private List<CloudInstance> getStopped(List<CloudVmInstanceStatus> statuses) {
        return statuses.stream().filter(status -> status.getStatus() == InstanceStatus.STOPPED)
                .map(CloudVmInstanceStatus::getCloudInstance).collect(Collectors.toList());
    }

    private List<CloudInstance> getStarted(List<CloudVmInstanceStatus> statuses) {
        return statuses.stream().filter(status -> status.getStatus() == InstanceStatus.STARTED)
                .map(CloudVmInstanceStatus::getCloudInstance).collect(Collectors.toList());
    }

    private List<CloudInstance> getNotExcludedStatusInstances(List<CloudVmInstanceStatus> statuses, List<InstanceStatus> excludedStatuses) {
        return statuses.stream().filter(status -> !excludedStatuses.contains(status.getStatus()))
                .map(CloudVmInstanceStatus::getCloudInstance).collect(Collectors.toList());
    }

    private List<CloudInstance> getExcludedStatusInstances(List<CloudVmInstanceStatus> statuses, List<InstanceStatus> excludedStatuses) {
        return statuses.stream().filter(status -> excludedStatuses.contains(status.getStatus()))
                .map(CloudVmInstanceStatus::getCloudInstance).collect(Collectors.toList());
    }

    public void logInvalidStatuses(List<CloudVmInstanceStatus> instances, InstanceStatus targetStatus) {
        if (CollectionUtils.isNotEmpty(instances)) {
            StringBuilder warnMessage = new StringBuilder("Unable to reboot ");
            warnMessage.append(instances.stream().map(instance -> String.format("instance %s because of invalid status %s",
                    instance.getCloudInstance().getInstanceId(), instance.getStatus().toString())).collect(Collectors.joining(", ")));
            warnMessage.append(String.format(". Instances should be in %s state.", targetStatus.toString()));
            LOGGER.warn(warnMessage.toString());
        }
    }

    private void handleEC2Exception(List<CloudInstance> vms, Ec2Exception e) {
        LOGGER.debug("Exception received from AWS: ", e);
        if (e.awsErrorDetails().errorCode().equalsIgnoreCase(INSTANCE_NOT_FOUND)) {
            Pattern pattern = Pattern.compile("i-[a-z0-9]*");
            Matcher matcher = pattern.matcher(e.awsErrorDetails().errorMessage());
            if (matcher.find()) {
                String doesNotExistInstanceId = matcher.group();
                LOGGER.debug("Remove instance from vms: {}", doesNotExistInstanceId);
                vms.removeIf(vm -> doesNotExistInstanceId.equals(vm.getInstanceId()));
            }
        } else if (e.awsErrorDetails().errorCode().equalsIgnoreCase(INSUFFICIENT_INSTANCE_CAPACITY)) {
            LOGGER.error("Encountered  EC2 insufficient capacity exception");
            throw new InsufficientCapacityException(e.getMessage(), e.getCause());
        }
        throw e;
    }

    @Retryable(
            value = SdkClientException.class,
            maxAttemptsExpression = "#{${cb.vm.retry.attempt:15}}",
            backoff = @Backoff(delayExpression = "#{${cb.vm.retry.backoff.delay:1000}}",
                    multiplierExpression = "#{${cb.vm.retry.backoff.multiplier:2}}",
                    maxDelayExpression = "#{${cb.vm.retry.backoff.maxdelay:10000}}")
    )
    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudInstance> vms) {
        return checkWithoutRetry(ac, vms);
    }

    @Override
    public List<CloudVmInstanceStatus> checkWithoutRetry(AuthenticatedContext ac, List<CloudInstance> vms) {
        LOGGER.debug("Check instances on aws side: {}", vms);
        List<CloudInstance> cloudInstancesWithInstanceId = vms.stream()
                .filter(cloudInstance -> cloudInstance.getInstanceId() != null)
                .collect(Collectors.toList());
        LOGGER.debug("Instances with instanceId: {}", cloudInstancesWithInstanceId);

        List<String> instanceIds = cloudInstancesWithInstanceId.stream().map(CloudInstance::getInstanceId)
                .collect(Collectors.toList());

        String region = ac.getCloudContext().getLocation().getRegion().value();
        try {
            DescribeInstancesRequest.Builder describeInstancesRequestBuilder = DescribeInstancesRequest.builder();
            if (CollectionUtils.isNotEmpty(instanceIds)) {
                describeInstancesRequestBuilder.instanceIds(instanceIds);
            }
            DescribeInstancesResponse result = new AuthenticatedContextView(ac).getAmazonEC2Client()
                    .describeInstances(describeInstancesRequestBuilder.build());
            LOGGER.debug("Response received from AWS for the following instances: {}", instanceIds);
            return fillCloudVmInstanceStatuses(ac, cloudInstancesWithInstanceId, region, result);
        } catch (Ec2Exception e) {
            handleEC2Exception(vms, e);
        } catch (SdkClientException e) {
            LOGGER.warn("Failed to send request to AWS: ", e);
            throw e;
        }
        return Collections.emptyList();
    }

    private List<CloudVmInstanceStatus> fillCloudVmInstanceStatuses(AuthenticatedContext ac, List<CloudInstance> cloudIntancesWithInstanceId, String region,
            DescribeInstancesResponse result) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (CloudInstance cloudInstance : cloudIntancesWithInstanceId) {
            Optional<Instance> instanceForInstanceId = result.reservations().stream()
                    .flatMap(reservation -> reservation.instances().stream())
                    .filter(instance -> instance.instanceId().equals(cloudInstance.getInstanceId()))
                    .findFirst();
            if (instanceForInstanceId.isPresent()) {
                Instance instance = instanceForInstanceId.get();
                LOGGER.debug("AWS instance [{}] is in {} state, region: {}, stack: {}",
                        instance.instanceId(), instance.state().name(), region, ac.getCloudContext().getId());
                cloudVmInstanceStatuses.add(new CloudVmInstanceStatus(cloudInstance,
                        AwsInstanceStatusMapper.getInstanceStatusByAwsStateAndReason(instance.state(), instance.stateReason())));
            } else {
                cloudVmInstanceStatuses.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.TERMINATED));
            }
        }
        return cloudVmInstanceStatuses;
    }

    private Collection<String> instanceIdsWhichAreNotInCorrectState(List<CloudInstance> vms, AmazonEc2Client amazonEC2Client, String state,
            Set<InstanceStatus> completedStatuses) {
        Set<String> instances = vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toSet());
        DescribeInstancesResponse describeInstances = amazonEC2Client.describeInstances(
                DescribeInstancesRequest.builder().instanceIds(instances).build());
        for (Reservation reservation : describeInstances.reservations()) {
            for (Instance instance : reservation.instances()) {
                if (state.equalsIgnoreCase(instance.state().name().toString().toLowerCase(Locale.ROOT))
                        || completedStatuses.contains(AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(instance.state().name().toString()))) {
                    LOGGER.debug(" Removing AWS instance [{}] with {} state from list of stop/start instances.",
                            instance.instanceId(), instance.state().name());
                    instances.remove(instance.instanceId());
                }
            }
        }
        return instances;
    }
}

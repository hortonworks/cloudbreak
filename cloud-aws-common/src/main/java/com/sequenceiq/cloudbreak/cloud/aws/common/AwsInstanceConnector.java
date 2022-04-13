package com.sequenceiq.cloudbreak.cloud.aws.common;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.GetConsoleOutputRequest;
import com.amazonaws.services.ec2.model.GetConsoleOutputResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.poller.PollerUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsInstanceStatusMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Service
public class AwsInstanceConnector implements InstanceConnector {

    public static final String INSTANCE_NOT_FOUND_ERROR_CODE = "InvalidInstanceID.NotFound";

    private static final int CHECK_STATUS_BY_DESCRIBE_INSTANCE_STATUS_THRESHOLD = 100;

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
        GetConsoleOutputRequest getConsoleOutputRequest = new GetConsoleOutputRequest().withInstanceId(vm.getInstanceId());
        GetConsoleOutputResult getConsoleOutputResult = amazonEC2Client.getConsoleOutput(getConsoleOutputRequest);
        try {
            return getConsoleOutputResult.getOutput() == null ? "" : getConsoleOutputResult.getDecodedOutput();
        } catch (Exception ex) {
            LOGGER.warn(ex.getMessage(), ex);
            return "";
        }
    }

    @Retryable(
            value = SdkClientException.class,
            maxAttemptsExpression = "#{${cb.vm.retry.attempt:15}}",
            backoff = @Backoff(delayExpression = "#{${cb.vm.retry.backoff.delay:1000}}",
                    multiplierExpression = "#{${cb.vm.retry.backoff.multiplier:2}}",
                    maxDelayExpression = "#{${cb.vm.retry.backoff.maxdelay:10000}}")
    )
    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        return setCloudVmInstanceStatuses(ac, vms, "Running",
                (ec2Client, instances) -> ec2Client.startInstances(new StartInstancesRequest().withInstanceIds(instances)),
                "Failed to send start request to AWS: ");
    }

    @Retryable(
            value = SdkClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000)
    )
    @Override
    public List<CloudVmInstanceStatus> startWithLimitedRetry(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms,
            Long timeboundInMs) {
        // The following states will never transition over to "Running"/STARTED - so it is safe to ignore instances in this state.
        Set<InstanceStatus> completedStatuses = EnumSet.of(
                InstanceStatus.FAILED,
                InstanceStatus.TERMINATED,
                InstanceStatus.TERMINATED_BY_PROVIDER,
                InstanceStatus.DELETE_REQUESTED,
                AwsInstanceStatusMapper.getInstanceStatusByAwsStatus("Running"));
        return setCloudVmInstanceStatuses(ac, vms, "Running",
                (ec2Client, instances) -> ec2Client.startInstances(new StartInstancesRequest().withInstanceIds(instances)),
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
                (ec2Client, instances) -> ec2Client.stopInstances(new StopInstancesRequest().withInstanceIds(instances)),
                "Failed to send stop request to AWS: ");
    }

    @Retryable(
            value = SdkClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000)
    )
    @Override
    public List<CloudVmInstanceStatus> stopWithLimitedRetry(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms,
            Long timeboundInMs) {
        Set<InstanceStatus> completedStatuses = EnumSet.of(
                InstanceStatus.FAILED,
                InstanceStatus.TERMINATED,
                InstanceStatus.TERMINATED_BY_PROVIDER,
                InstanceStatus.DELETE_REQUESTED,
                AwsInstanceStatusMapper.getInstanceStatusByAwsStatus("Stopped"));
        return setCloudVmInstanceStatuses(ac, vms, "Stopped",
                (ec2Client, instances) -> ec2Client.stopInstances(new StopInstancesRequest().withInstanceIds(instances)),
                completedStatuses,
                "Failed to send stop request to AWS: ", timeboundInMs);
    }

    private List<CloudVmInstanceStatus> setCloudVmInstanceStatuses(AuthenticatedContext ac, List<CloudInstance> vms, String status,
            BiConsumer<AmazonEc2Client, Collection<String>> consumer, Set<InstanceStatus> completedStatuses, String exceptionText,
            Long timeboundInMs) {
        AmazonEc2Client amazonEC2Client = new AuthenticatedContextView(ac).getAmazonEC2Client();
        try {
            Collection<String> instances = getInstanceIdsNotInState(vms, amazonEC2Client, status);
            if (!instances.isEmpty()) {
                consumer.accept(amazonEC2Client, instances);
            }
        } catch (AmazonEC2Exception e) {
            handleEC2Exception(vms, e);
        } catch (SdkClientException e) {
            LOGGER.warn(exceptionText, e);
            throw e;
        }
        if (timeboundInMs != null) {
            return pollerUtil.timeBoundWaitFor(timeboundInMs, ac, vms, completedStatuses, status);
        } else {
            return pollerUtil.waitFor(ac, vms, completedStatuses, status);
        }
    }

    private List<CloudVmInstanceStatus> setCloudVmInstanceStatuses(AuthenticatedContext ac, List<CloudInstance> vms, String status,
            BiConsumer<AmazonEc2Client, Collection<String>> consumer, String exceptionText) {
        return setCloudVmInstanceStatuses(ac, vms, status, consumer,
                Sets.newHashSet(AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(status), InstanceStatus.FAILED), exceptionText, null);
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
                doStop(ac, getStarted(statuses));
                statuses = check(ac, vms);
                logInvalidStatuses(getNotStopped(statuses), InstanceStatus.STOPPED);
                rebootedVmsStatus = doStart(ac, getStopped(statuses));
                logInvalidStatuses(getNotStarted(statuses), InstanceStatus.STARTED);
            }
        } catch (SdkClientException e) {
            LOGGER.warn("Failed to send reboot request to AWS: ", e);
            throw e;
        }
        return rebootedVmsStatus;
    }

    private List<CloudVmInstanceStatus> getNotStarted(List<CloudVmInstanceStatus> statuses) {
        return statuses.stream().filter(status -> status.getStatus() != InstanceStatus.STARTED)
                .collect(toList());
    }

    private List<CloudVmInstanceStatus> getNotStopped(List<CloudVmInstanceStatus> statuses) {
        return statuses.stream().filter(status -> status.getStatus() != InstanceStatus.STOPPED)
                .collect(toList());
    }

    private List<CloudInstance> getStopped(List<CloudVmInstanceStatus> statuses) {
        return statuses.stream().filter(status -> status.getStatus() == InstanceStatus.STOPPED)
                .map(CloudVmInstanceStatus::getCloudInstance).collect(toList());
    }

    private List<CloudInstance> getStarted(List<CloudVmInstanceStatus> statuses) {
        return statuses.stream().filter(status -> status.getStatus() == InstanceStatus.STARTED)
                .map(CloudVmInstanceStatus::getCloudInstance).collect(toList());
    }

    private List<CloudVmInstanceStatus> doStart(AuthenticatedContext ac, List<CloudInstance> instances) {
        List<CloudVmInstanceStatus> rebootedVmsStatus = new ArrayList<>();
        for (CloudInstance instance : instances) {
            try {
                rebootedVmsStatus.addAll(start(ac, null, List.of(instance)));
            } catch (AmazonEC2Exception e) {
                LOGGER.warn(String.format("Unable to start instance %s", instance), e);
            }
        }
        return rebootedVmsStatus;
    }

    private void doStop(AuthenticatedContext ac, List<CloudInstance> instances) {
        for (CloudInstance instance : instances) {
            try {
                stop(ac, null, List.of(instance));
            } catch (AmazonEC2Exception e) {
                LOGGER.warn(String.format("Unable to stop instance %s", instance), e);
            }
        }
    }

    public void logInvalidStatuses(List<CloudVmInstanceStatus> instances, InstanceStatus targetStatus) {
        if (CollectionUtils.isNotEmpty(instances)) {
            StringBuilder warnMessage = new StringBuilder("Unable to reboot ");
            warnMessage.append(instances.stream().map(instance -> String.format("instance %s because of invalid status %s",
                    instance.getCloudInstance().getInstanceId(), instance.getStatus().toString())).collect(joining(", ")));
            warnMessage.append(String.format(". Instances should be in %s state.", targetStatus.toString()));
            LOGGER.warn(warnMessage.toString());
        }
    }

    private void handleEC2Exception(List<CloudInstance> vms, AmazonEC2Exception e) throws AmazonEC2Exception {
        LOGGER.debug("Exception received from AWS: ", e);
        if (e.getErrorCode().equalsIgnoreCase(INSTANCE_NOT_FOUND_ERROR_CODE)) {
            Pattern pattern = Pattern.compile("i-[a-z0-9]*");
            Matcher matcher = pattern.matcher(e.getErrorMessage());
            if (matcher.find()) {
                String doesNotExistInstanceId = matcher.group();
                LOGGER.debug("Remove instance from vms: {}", doesNotExistInstanceId);
                vms.removeIf(vm -> doesNotExistInstanceId.equals(vm.getInstanceId()));
            }
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
        try {
            LOGGER.debug("Check instances on aws side: {}", vms);
            AmazonEc2Client amazonEc2Client = new AuthenticatedContextView(ac).getAmazonEC2Client();
            return getInstanceStatuses(amazonEc2Client, vms);
        } catch (AmazonEC2Exception e) {
            handleEC2Exception(vms, e);
        } catch (SdkClientException e) {
            LOGGER.warn("Failed to send request to AWS: ", e);
            throw e;
        }
        return Collections.emptyList();
    }

    private Collection<String> getInstanceIdsNotInState(List<CloudInstance> vms, AmazonEc2Client amazonEC2Client, String state) {
        Set<String> instances = vms.stream().map(CloudInstance::getInstanceId).collect(toCollection(HashSet::new));
        describeInstanceStatuses(amazonEC2Client, instances)
                .forEach(instanceStatus -> {
                    if (state.equalsIgnoreCase(instanceStatus.getInstanceState().getName())) {
                        instances.remove(instanceStatus.getInstanceId());
                    }
                });
        return instances;
    }

    private List<CloudVmInstanceStatus> getInstanceStatuses(AmazonEc2Client amazonEc2Client, List<CloudInstance> vms) {
        Map<String, CloudInstance> cloudInstanceMap = sortById(vms);
        List<com.amazonaws.services.ec2.model.InstanceStatus> instanceStatuses = describeInstanceStatuses(amazonEc2Client, cloudInstanceMap.keySet());
        List<Instance> instances = describeInstances(amazonEc2Client, instanceStatuses.stream()
                .filter(instanceStatus -> AwsInstanceStatusMapper.isTerminated(instanceStatus.getInstanceState()))
                .map(com.amazonaws.services.ec2.model.InstanceStatus::getInstanceId)
                .collect(toList()));
        List<CloudVmInstanceStatus> result = new ArrayList<>();
        result.addAll(instanceStatuses.stream()
                .filter(instanceStatus -> !AwsInstanceStatusMapper.isTerminated(instanceStatus.getInstanceState()))
                .map(instanceStatus -> toCloudInstanceStatus(instanceStatus, cloudInstanceMap.get(instanceStatus.getInstanceId())))
                .collect(toList()));
        result.addAll(instances
                .stream()
                .map(instance -> toCloudInstanceStatus(instance, cloudInstanceMap.get(instance.getInstanceId())))
                .collect(toList()));
        return result;
    }

    private Map<String, CloudInstance> sortById(List<CloudInstance> vms) {
        Map<String, CloudInstance> instancesById = new LinkedHashMap<>();
        for (CloudInstance instance : vms) {
            if (instance.getInstanceId() != null && !instancesById.containsKey(instance.getInstanceId())) {
                instancesById.put(instance.getInstanceId(), instance);
            }
        }
        return instancesById;
    }

    private List<Instance> describeInstances(AmazonEc2Client amazonEc2Client, Collection<String> instanceIds) {
        if (instanceIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Instance> instances = amazonEc2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIds))
                .getReservations()
                .stream()
                .map(Reservation::getInstances)
                .flatMap(Collection::stream)
                .collect(toList());
        LOGGER.debug("Instances from AWS: {}", instances);
        return instances;
    }

    private List<com.amazonaws.services.ec2.model.InstanceStatus> describeInstanceStatuses(AmazonEc2Client amazonEc2Client, Set<String> instanceIds) {
        if (instanceIds.isEmpty()) {
            return new ArrayList<>();
        }
        AtomicInteger partitionCounter = new AtomicInteger();
        List<com.amazonaws.services.ec2.model.InstanceStatus> instanceStatuses = instanceIds
                .stream()
                .collect(groupingBy(it -> partitionCounter.getAndIncrement() / CHECK_STATUS_BY_DESCRIBE_INSTANCE_STATUS_THRESHOLD))
                .values()
                .stream()
                .map(ids -> amazonEc2Client.describeInstanceStatuses(
                        new DescribeInstanceStatusRequest()
                                .withInstanceIds(ids)
                                .withIncludeAllInstances(true)))
                .map(DescribeInstanceStatusResult::getInstanceStatuses)
                .flatMap(Collection::stream)
                .collect(toList());
        LOGGER.debug("Instance status results from AWS: {}", instanceStatuses);
        return instanceStatuses;
    }

    private CloudVmInstanceStatus toCloudInstanceStatus(com.amazonaws.services.ec2.model.InstanceStatus instanceStatus, CloudInstance cloudInstance) {
        return new CloudVmInstanceStatus(cloudInstance, AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(instanceStatus.getInstanceState().getName()));
    }

    private CloudVmInstanceStatus toCloudInstanceStatus(Instance instance, CloudInstance cloudInstance) {
        return new CloudVmInstanceStatus(cloudInstance,
                AwsInstanceStatusMapper.getInstanceStatusByAwsStateAndReason(instance.getState(), instance.getStateReason()));
    }
}

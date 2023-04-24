package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.poller.PollerUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsInstanceStatusMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.exception.InsufficientCapacityException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

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

    public static final String INSTANCE_NOT_FOUND_ERROR_CODE = "InvalidInstanceID.NotFound";

    public static final String INSUFFICIENT_INSTANCE_CAPACITY_ERROR_CODE = "InsufficientInstanceCapacity";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceConnector.class);

    private static final Set<InstanceStatus> PENDING_STATUSES = EnumSet.of(
            InstanceStatus.PENDING);

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
            return getConsoleOutputResponse.output() == null ? "" : new String(Base64.getDecoder().decode(getConsoleOutputResponse.output()));
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
        LOGGER.debug("Sending start request for VM(s) with the following ID(s): {}", String.join(",",
                vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList())));
        return setCloudVmInstanceStatuses(ac, vms, "Running",
                (ec2Client, instances) -> ec2Client.startInstances(StartInstancesRequest.builder().instanceIds(instances).build()),
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
        LOGGER.debug("Sending start request for VM(s) with the following ID(s): {}", String.join(",",
                vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList())));
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
        try {
            ImmutablePair<Collection<String>, Collection<String>> instancesPair =
                    instanceIdsWhichAreNotInCorrectState(vms, amazonEC2Client, status, completedStatuses);
            Collection<String> instances = instancesPair.getLeft();
            Collection<String> pendingInstances = instancesPair.getRight();
            if (!instances.isEmpty()) {
                consumer.accept(amazonEC2Client, instances);
            }
            if (!pendingInstances.isEmpty()) {
                LOGGER.debug("Force Stopping Instances Stuck on PENDING state in AWS: {}", pendingInstances);
                forceStopPendingInstances(amazonEC2Client, pendingInstances);
                vms = vms.stream().filter(i -> !pendingInstances.contains(i.getInstanceId())).collect(Collectors.toList());
            }
        } catch (Ec2Exception e) {
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

    private void forceStopPendingInstances(AmazonEc2Client amazonEC2Client, Collection<String> pendingInstances) {
        amazonEC2Client.stopInstances(StopInstancesRequest.builder().force(true).instanceIds(pendingInstances).build());
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

    private List<CloudVmInstanceStatus> doStart(AuthenticatedContext ac, List<CloudInstance> instances) {
        List<CloudVmInstanceStatus> rebootedVmsStatus = new ArrayList<>();
        for (CloudInstance instance : instances) {
            try {
                rebootedVmsStatus.addAll(start(ac, null, List.of(instance)));
            } catch (Ec2Exception e) {
                LOGGER.warn(String.format("Unable to start instance %s", instance), e);
            }
        }
        return rebootedVmsStatus;
    }

    private void doStop(AuthenticatedContext ac, List<CloudInstance> instances) {
        for (CloudInstance instance : instances) {
            try {
                stop(ac, null, List.of(instance));
            } catch (Ec2Exception e) {
                LOGGER.warn(String.format("Unable to stop instance %s", instance), e);
            }
        }
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
        if (e.awsErrorDetails().errorCode().equalsIgnoreCase(INSTANCE_NOT_FOUND_ERROR_CODE)) {
            Pattern pattern = Pattern.compile("i-[a-z0-9]*");
            Matcher matcher = pattern.matcher(e.awsErrorDetails().errorMessage());
            if (matcher.find()) {
                String doesNotExistInstanceId = matcher.group();
                LOGGER.debug("Remove instance from vms: {}", doesNotExistInstanceId);
                vms.removeIf(vm -> doesNotExistInstanceId.equals(vm.getInstanceId()));
            }
        } else if (e.awsErrorDetails().errorCode().equalsIgnoreCase(INSUFFICIENT_INSTANCE_CAPACITY_ERROR_CODE)) {
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
            LOGGER.debug("Response from AWS: {}", result);
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
        for (Reservation reservation : result.reservations()) {
            for (Instance instance : reservation.instances()) {
                Optional<CloudInstance> cloudInstanceForInstanceId = cloudIntancesWithInstanceId.stream()
                        .filter(cloudInstance -> cloudInstance.getInstanceId().equals(instance.instanceId()))
                        .findFirst();
                if (cloudInstanceForInstanceId.isPresent()) {
                    CloudInstance cloudInstance = cloudInstanceForInstanceId.get();
                    LOGGER.debug("AWS instance [{}] is in {} state, region: {}, stack: {}",
                            instance.instanceId(), instance.state().name(), region, ac.getCloudContext().getId());
                    cloudVmInstanceStatuses.add(new CloudVmInstanceStatus(cloudInstance,
                            AwsInstanceStatusMapper.getInstanceStatusByAwsStateAndReason(instance.state(), instance.stateReason())));
                }
            }
        }
        return cloudVmInstanceStatuses;
    }

    private ImmutablePair<Collection<String>, Collection<String>> instanceIdsWhichAreNotInCorrectState(List<CloudInstance> vms,
            AmazonEc2Client amazonEC2Client, String state, Set<InstanceStatus> completedStatuses) {
        Set<String> instances = vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toCollection(HashSet::new));
        Set<String> pendingInstances = vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toCollection(HashSet::new));
        DescribeInstancesResponse describeInstances = amazonEC2Client.describeInstances(
                DescribeInstancesRequest.builder().instanceIds(instances).build());
        for (Reservation reservation : describeInstances.reservations()) {
            for (Instance instance : reservation.instances()) {
                String instanceState = instance.state().name().toString();
                if (state.equalsIgnoreCase(instanceState) || PENDING_STATUSES.contains(AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(instanceState))
                        || completedStatuses.contains(AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(instanceState))) {
                    LOGGER.debug(" Removing AWS instance [{}] with {} state from list of stop/start instances.",
                            instance.instanceId(), instance.state().name());
                    instances.remove(instance.instanceId());
                }
                if (!PENDING_STATUSES.contains(AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(instanceState))) {
                    pendingInstances.remove(instance.instanceId());
                }
            }
        }
        return ImmutablePair.of(instances, pendingInstances);
    }
}

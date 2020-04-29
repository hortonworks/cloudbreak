package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.GetConsoleOutputRequest;
import com.amazonaws.services.ec2.model.GetConsoleOutputResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RebootInstancesRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.poller.PollerUtil;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsInstanceStatusMapper;
import com.sequenceiq.cloudbreak.cloud.aws.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Service
public class AwsInstanceConnector implements InstanceConnector {

    public static final String INSTANCE_NOT_FOUND_ERROR_CODE = "InvalidInstanceID.NotFound";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceConnector.class);

    @Inject
    private PollerUtil pollerUtil;

    @Inject
    private AwsClient awsClient;

    @Value("${cb.aws.hostkey.verify:}")
    private boolean verifyHostKey;

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        if (!verifyHostKey) {
            throw new CloudOperationNotSupportedException("Host key verification is disabled on AWS");
        }
        AmazonEC2Client amazonEC2Client = new AuthenticatedContextView(authenticatedContext).getAmazonEC2Client();
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

    private List<CloudVmInstanceStatus> setCloudVmInstanceStatuses(AuthenticatedContext ac, List<CloudInstance> vms, String status,
            BiConsumer<AmazonEC2Client, Collection<String>> consumer, String exceptionText) {
        AmazonEC2Client amazonEC2Client = new AuthenticatedContextView(ac).getAmazonEC2Client();
        try {
            Collection<String> instances = instanceIdsWhichAreNotInCorrectState(vms, amazonEC2Client, status);
            if (!instances.isEmpty()) {
                consumer.accept(amazonEC2Client, instances);
            }
        } catch (AmazonEC2Exception e) {
            handleEC2Exception(vms, e);
        } catch (SdkClientException e) {
            LOGGER.warn(exceptionText, e);
            throw e;
        }
        return pollerUtil.waitFor(ac, vms, Sets.newHashSet(AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(status), InstanceStatus.FAILED));
    }

    @Retryable(
            value = SdkClientException.class,
            maxAttempts = 15,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    @Override
    public List<CloudVmInstanceStatus> reboot(AuthenticatedContext ac, List<CloudInstance> vms) {
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        List<CloudInstance> affectedVms = new ArrayList<>();
        try {
            if (!vms.isEmpty()) {
                List<CloudVmInstanceStatus> statuses = check(ac, vms);
                doReboot(affectedVms, amazonEC2Client, getStarted(statuses));
                doStart(affectedVms, ac, getStopped(statuses));
                logInvalidStatuses(getNotStoppedOrStarted(statuses));
            }
        } catch (SdkClientException e) {
            LOGGER.warn("Failed to send reboot request to AWS: ", e);
            throw e;
        }
        return pollerUtil.waitFor(ac, affectedVms, Sets.newHashSet(InstanceStatus.STARTED, InstanceStatus.FAILED));
    }

    private List<CloudVmInstanceStatus> getNotStoppedOrStarted(List<CloudVmInstanceStatus> statuses) {
        return statuses.stream().filter(status -> status.getStatus() != InstanceStatus.STOPPED && status.getStatus() != InstanceStatus.STARTED)
                .collect(Collectors.toList());
    }

    private List<CloudInstance> getStopped(List<CloudVmInstanceStatus> statuses) {
        return statuses.stream().filter(status -> status.getStatus() == InstanceStatus.STOPPED)
                .map(status -> status.getCloudInstance()).collect(Collectors.toList());
    }

    private List<CloudInstance> getStarted(List<CloudVmInstanceStatus> statuses) {
        return statuses.stream().filter(status -> status.getStatus() == InstanceStatus.STARTED)
                .map(status -> status.getCloudInstance()).collect(Collectors.toList());
    }

    private void doReboot(List<CloudInstance> affectedVMs, AmazonEC2Client amazonEC2Client, List<CloudInstance> instances) {
        for (CloudInstance instance: instances) {
            try {
                amazonEC2Client.rebootInstances(new RebootInstancesRequest().withInstanceIds(List.of(instance.getInstanceId())));
                affectedVMs.add(instance);
            } catch (AmazonEC2Exception e) {
                LOGGER.warn(String.format("Unable to reboot instance %s", instance), e);
            }
        }
    }

    private void doStart(List<CloudInstance> affectedVMs, AuthenticatedContext ac, List<CloudInstance> instances) {
        for (CloudInstance instance: instances) {
            try {
                start(ac, null, List.of(instance));
                affectedVMs.add(instance);
            } catch (AmazonEC2Exception e) {
                LOGGER.warn(String.format("Unable to start instance %s", instance), e);
            }
        }
    }

    private void logInvalidStatuses(List<CloudVmInstanceStatus> instances) {
        for (CloudVmInstanceStatus instance: instances) {
            LOGGER.warn(String.format("Unable to reboot instance %s because of invalid status %s.",
                    instance.getCloudInstance().getInstanceId(), instance.getStatus().toString()));
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
        LOGGER.debug("Check instances on aws side: {}", vms);
        List<CloudInstance> cloudInstancesWithInstanceId = vms.stream()
                .filter(cloudInstance -> cloudInstance.getInstanceId() != null)
                .collect(Collectors.toList());
        LOGGER.debug("Instances with instanceId: {}", cloudInstancesWithInstanceId);

        List<String> instanceIds = cloudInstancesWithInstanceId.stream().map(CloudInstance::getInstanceId)
                .collect(Collectors.toList());

        String region = ac.getCloudContext().getLocation().getRegion().value();
        try {
            DescribeInstancesResult result = new AuthenticatedContextView(ac).getAmazonEC2Client()
                    .describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIds));
            LOGGER.debug("Result from AWS: {}", result);
            return fillCloudVmInstanceStatuses(ac, cloudInstancesWithInstanceId, region, result);
        } catch (AmazonEC2Exception e) {
            handleEC2Exception(vms, e);
        } catch (SdkClientException e) {
            LOGGER.warn("Failed to send request to AWS: ", e);
            throw e;
        }
        return Collections.emptyList();
    }

    private List<CloudVmInstanceStatus> fillCloudVmInstanceStatuses(AuthenticatedContext ac, List<CloudInstance> cloudIntancesWithInstanceId, String region,
            DescribeInstancesResult result) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (Reservation reservation : result.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                Optional<CloudInstance> cloudInstanceForInstanceId = cloudIntancesWithInstanceId.stream()
                        .filter(cloudInstance -> cloudInstance.getInstanceId().equals(instance.getInstanceId()))
                        .findFirst();
                if (cloudInstanceForInstanceId.isPresent()) {
                    CloudInstance cloudInstance = cloudInstanceForInstanceId.get();
                    LOGGER.debug("AWS instance [{}] is in {} state, region: {}, stack: {}",
                            instance.getInstanceId(), instance.getState().getName(), region, ac.getCloudContext().getId());
                    cloudVmInstanceStatuses.add(new CloudVmInstanceStatus(cloudInstance,
                            AwsInstanceStatusMapper.getInstanceStatusByAwsStateAndReason(instance.getState(), instance.getStateReason())));
                }
            }
        }
        return cloudVmInstanceStatuses;
    }

    private Collection<String> instanceIdsWhichAreNotInCorrectState(List<CloudInstance> vms, AmazonEC2 amazonEC2Client, String state) {
        Set<String> instances = vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toCollection(HashSet::new));
        DescribeInstancesResult describeInstances = amazonEC2Client.describeInstances(
                new DescribeInstancesRequest().withInstanceIds(instances));
        for (Reservation reservation : describeInstances.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                if (state.equalsIgnoreCase(instance.getState().getName())) {
                    instances.remove(instance.getInstanceId());
                }
            }
        }
        return instances;
    }
}

package com.sequenceiq.cloudbreak.cloud.gcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Instances.GetSerialPortOutput;
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.template.AbstractInstanceConnector;

@Service
public class GcpInstanceConnector extends AbstractInstanceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceConnector.class);

    @Value("${cb.gcp.hostkey.verify:}")
    private boolean verifyHostKey;

    @Inject
    private GcpComputeFactory computeClient;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Override
    public List<CloudVmInstanceStatus> reboot(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> rebootedVmsStatus = new ArrayList<>();

        try {
            if (!vms.isEmpty()) {
                List<CloudVmInstanceStatus> statuses = check(ac, vms);
                List<CloudInstance> startedInstances = getStarted(statuses);
                LOGGER.info("Already Stopped Vms on cloudProvider: {}", getNotStarted(statuses));
                if (!startedInstances.isEmpty()) {
                    stop(ac, resources, getStarted(statuses));
                    statuses = check(ac, vms);
                }
                logInvalidStatuses(getNotStopped(statuses), InstanceStatus.STOPPED);
                List<CloudInstance> stoppedInstances = getStopped(statuses);
                LOGGER.info("Already Started Vms on cloudProvider: {}", getNotStopped(statuses));
                if (!stoppedInstances.isEmpty()) {
                    rebootedVmsStatus = start(ac, resources, getStopped(statuses));
                }
                logInvalidStatuses(getNotStarted(statuses), InstanceStatus.STARTED);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to send reboot request to Google Cloud: ", e);
            throw e;
        }
        return rebootedVmsStatus;
    }

    @Override
    public List<CloudVmInstanceStatus> restartWithLimitedRetry(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms,
            Long timeboundInMs, List<InstanceStatus> excludedStatuses) {
        LOGGER.info("Restarting vms on GCP: {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        List<CloudVmInstanceStatus> rebootedVmsStatus = new ArrayList<>();

        try {
            if (!vms.isEmpty()) {
                List<CloudVmInstanceStatus> statuses = check(ac, vms);
                List<CloudInstance> notExcludedStartedInstances = getNotExcludedStatusInstances(statuses, excludedStatuses);
                LOGGER.info("Vms of excluded statuses on cloudProvider: {}", getExcludedStatusInstances(statuses, excludedStatuses));
                if (!notExcludedStartedInstances.isEmpty()) {
                    stop(ac, resources, getStarted(statuses));
                    statuses = check(ac, vms);
                }
                logInvalidStatuses(getNotStopped(statuses), InstanceStatus.STOPPED);
                List<CloudInstance> stoppedInstances = getStopped(statuses);
                LOGGER.info("Already Started Vms on cloudProvider: {}", getNotStopped(statuses));
                if (!stoppedInstances.isEmpty()) {
                    rebootedVmsStatus = start(ac, resources, getStopped(statuses));
                }
                logInvalidStatuses(getNotStarted(rebootedVmsStatus), InstanceStatus.STARTED);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to send reboot request to Google Cloud: ", e);
            throw e;
        }
        return rebootedVmsStatus;
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

    private List<CloudInstance> getNotExcludedStatusInstances(List<CloudVmInstanceStatus> statuses, List<InstanceStatus> excludedStatuses) {
        return statuses.stream().filter(status -> !excludedStatuses.contains(status.getStatus()))
                .map(CloudVmInstanceStatus::getCloudInstance).collect(Collectors.toList());
    }

    private List<CloudInstance> getExcludedStatusInstances(List<CloudVmInstanceStatus> statuses, List<InstanceStatus> excludedStatuses) {
        return statuses.stream().filter(status -> excludedStatuses.contains(status.getStatus()))
                .map(CloudVmInstanceStatus::getCloudInstance).collect(Collectors.toList());
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
                .map(status -> status.getCloudInstance()).collect(Collectors.toList());
    }

    private List<CloudInstance> getStarted(List<CloudVmInstanceStatus> statuses) {
        return statuses.stream().filter(status -> status.getStatus() == InstanceStatus.STARTED)
                .map(status -> status.getCloudInstance()).collect(Collectors.toList());
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        CloudCredential credential = ac.getCloudCredential();
        CloudContext cloudContext = ac.getCloudContext();
        Compute compute = computeClient.buildCompute(credential);
        for (CloudInstance instance : vms) {
            InstanceStatus status = InstanceStatus.UNKNOWN;
            try {
                Instance executeInstance = getInstance(cloudContext, credential, compute, instance);
                if ("RUNNING".equals(executeInstance.getStatus())) {
                    status = InstanceStatus.STARTED;
                } else if ("TERMINATED".equals(executeInstance.getStatus())) {
                    status = InstanceStatus.STOPPED;
                } else {
                    LOGGER.info("Instance {} status is: {}", instance, executeInstance.getStatus());
                }
            } catch (GoogleJsonResponseException e) {
                if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    status = InstanceStatus.TERMINATED;
                } else {
                    LOGGER.info("Instance {} is not reachable", instance, e);
                }
            } catch (IOException e) {
                LOGGER.info("Instance {} is not reachable", instance, e);
            }
            statuses.add(new CloudVmInstanceStatus(instance, status));
        }
        return statuses;
    }

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        if (!verifyHostKey) {
            throw new CloudOperationNotSupportedException("Host key verification is disabled on GCP");
        }
        CloudCredential credential = authenticatedContext.getCloudCredential();
        try {
            GetSerialPortOutput instanceGet = computeClient.buildCompute(credential).instances()
                    .getSerialPortOutput(gcpStackUtil.getProjectId(credential),
                            vm.getAvailabilityZone(), vm.getInstanceId());
            return instanceGet.execute().getContents();
        } catch (Exception e) {
            throw new GcpResourceException("Couldn't parse SSH fingerprint from console output.", e);
        }
    }

    private Instance getInstance(CloudContext context, CloudCredential credential, Compute compute, CloudInstance instance) throws IOException {
        return compute.instances().get(gcpStackUtil.getProjectId(credential),
                instance.getAvailabilityZone(), instance.getInstanceId()).execute();
    }
}

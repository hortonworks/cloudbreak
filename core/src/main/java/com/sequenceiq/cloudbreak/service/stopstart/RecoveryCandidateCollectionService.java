package com.sequenceiq.cloudbreak.service.stopstart;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterHealthService;
import com.sequenceiq.cloudbreak.cluster.status.DetailedHostStatuses;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.converter.CloudInstanceIdToInstanceMetaDataConverter;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class RecoveryCandidateCollectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryCandidateCollectionService.class);

    @Value("#{'${nodemanager.irrecoverable.healthchecks}'.split(',')}")
    private Set<String> irrecoverableHealthChecks;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private RuntimeVersionService runtimeVersionService;

    @Inject
    private CloudInstanceIdToInstanceMetaDataConverter cloudInstanceIdToInstanceMetaDataConverter;

    public List<InstanceMetadataView> getStartedInstancesWithServicesNotRunning(StackDto stack, String hostGroupName,
            Set<String> startedInstanceIdsOnCloudProvider, boolean failureRecoveryEnabled) {
        ClusterHealthService clusterHealthService = clusterApiConnectors.getConnector(stack).clusterHealthService();

        if (clusterHealthService.isClusterManagerRunning()) {
            List<InstanceMetadataView> startedInstancesMetadata = cloudInstanceIdToInstanceMetaDataConverter.getNotDeletedAndNotZombieInstances(
                    stack.getAllAvailableInstances(),
                    hostGroupName,
                    startedInstanceIdsOnCloudProvider);

            DetailedHostStatuses detailedHostStatuses =
                    clusterHealthService.getDetailedHostStatuses(runtimeVersionService.getRuntimeVersion(stack.getCluster().getId()));

            LOGGER.info("DetailedHostsStatuses retrieved via CM API call for stack: {}", stack.getId());

            return collectRecoveryCandidates(startedInstancesMetadata,
                    detailedHostStatuses, runtimeVersionService.getRuntimeVersion(stack.getCluster().getId()), failureRecoveryEnabled);
        } else {
            return Collections.emptyList();
        }
    }

    private List<InstanceMetadataView> collectRecoveryCandidates(List<InstanceMetadataView> startedInstances, DetailedHostStatuses detailedHostStatuses,
            Optional<String> runtimeVersion, boolean failureRecoveryEnabled) {
        boolean servicesHealthCheckAllowed = CMRepositoryVersionUtil.isCmServicesHealthCheckAllowed(runtimeVersion);
        Predicate<InstanceMetadataView> instanceHealthCheck;
        if (failureRecoveryEnabled && servicesHealthCheckAllowed) {
            instanceHealthCheck = filterByIrrecoverableServiceHealth(detailedHostStatuses);
        } else {
            instanceHealthCheck = filterByServicesNotRunning(servicesHealthCheckAllowed, detailedHostStatuses);
        }
        return startedInstances.stream().filter(instanceHealthCheck).collect(Collectors.toList());
    }

    private Predicate<InstanceMetadataView> filterByIrrecoverableServiceHealth(DetailedHostStatuses detailedHostStatuses) {
        // service irrecoverable health : false,  Services Not Running: True -> Candidate
        return im -> {
            HostName hostName = hostName(im.getDiscoveryFQDN());
            boolean servicesNotRunning = detailedHostStatuses.areServicesNotRunning(hostName);
            boolean servicesUnHealthy = detailedHostStatuses.areServicesUnhealthy(hostName);
            boolean servicesIrrecoverable = detailedHostStatuses.areServicesIrrecoverable(hostName, irrecoverableHealthChecks);
            if (servicesIrrecoverable) {
                LOGGER.info("Did not collect: {} as recovery candidate as it has services which are unhealthy and irrecoverable.", hostName.getValue());
                return false;
            } else if (servicesNotRunning) {
                LOGGER.info("Collected: {} as recovery candidate with services not running.", hostName.getValue());
                return true;
            } else {
                if (servicesUnHealthy) {
                    LOGGER.info("Collected: {} as recovery candidate with services unhealthy.", hostName.getValue());
                }
                return servicesUnHealthy;
            }
        };
    }

    private Predicate<InstanceMetadataView> filterByServicesNotRunning(boolean servicesHealthCheckAllowed,
            DetailedHostStatuses detailedHostStatuses) {
        return im -> {
            HostName hostName = hostName(im.getDiscoveryFQDN());
            boolean hostUnhealthy = detailedHostStatuses.isHostUnHealthy(hostName);
            boolean hostDecommissioned = detailedHostStatuses.isHostDecommissioned(hostName);
            boolean servicesNotRunning = detailedHostStatuses.areServicesNotRunning(hostName);

            return servicesHealthCheckAllowed
                    ? getCandidacyPost7212(hostUnhealthy, hostDecommissioned, servicesNotRunning)
                    : getCandidacyPre7212(hostUnhealthy, hostDecommissioned);
        };
    }

    public Set<String> collectStartedInstancesFromCloudProvider(CloudConnector connector, AuthenticatedContext ac,
            List<CloudInstance> cloudInstances) {
        List<CloudVmInstanceStatus> vmInstanceStatuses = connector.instances().checkWithoutRetry(ac, cloudInstances);
        Set<String> startedInstanceIds = vmInstanceStatuses.stream()
                .filter(vm -> InstanceStatus.STARTED.equals(vm.getStatus()))
                .map(CloudVmInstanceStatus::getCloudInstance)
                .map(CloudInstance::getInstanceId)
                .collect(Collectors.toSet());
        LOGGER.info("Started instanceIds on cloudProvider: {}", startedInstanceIds);
        return startedInstanceIds;
    }

    private boolean getCandidacyPost7212(boolean hostUnhealthy, boolean hostDecommissioned, boolean servicesNotRunning) {
        // CM: Host Healthy, MaintMode: True, Services Not Running: True -> Candidate
        // CM: Host Healthy, MaintMode: False, Services Not Running: True -> Candidate
        return !hostUnhealthy && hostDecommissioned && servicesNotRunning;
    }

    private boolean getCandidacyPre7212(boolean hostUnhealthy, boolean hostDecommissioned) {
        // CM: Host Healthy, MaintMode: True, Services Not Running: Unknown -> Candidate
        // CM: Host Healthy, MaintMode: False, Services Not Running: Unknown -> Candidate
        return !hostUnhealthy && hostDecommissioned;
    }
}

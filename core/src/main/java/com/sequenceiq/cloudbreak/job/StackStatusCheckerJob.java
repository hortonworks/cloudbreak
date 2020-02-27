package com.sequenceiq.cloudbreak.job;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.gs.collections.impl.factory.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.InstanceStateQuery;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterFlowService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;
import com.sequenceiq.environment.client.EnvironmentInternalCrnClient;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.statuschecker.service.JobService;

@Component
public class StackStatusCheckerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusCheckerJob.class);

    @Inject
    private JobService jobService;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterFlowService clusterFlowService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Inject
    private InstanceStateQuery instanceStateQuery;

    @Inject
    private EnvironmentInternalCrnClient environmentInternalCrnClient;

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private CredentialToCloudCredentialConverter cloudCredentialConverter;

    @Inject
    private StackSyncService syncService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        if (flowLogService.isOtherFlowRunning(getStackId())) {
            LOGGER.debug("StackStatusCheckerJob cannot run, because flow is running for stack: {}", getStackId());
            return;
        }
        try {
            Stack stack = stackService.get(getStackId());
            buildMdcContext(stack);
            Set<InstanceMetaData> runningInstances = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
            if (stack.isStackInDeletionOrFailedPhase()) {
                LOGGER.debug("StackStatusCheckerJob cannot run, stack is being terminated: {}", getStackId());
                jobService.unschedule(getLocalId());
                return;
            }
            ClusterApi connector = clusterApiConnectors.getConnector(stack);
            try {
                if (isClusterManagerRunning(stack, connector)) {
                    Map<HostName, ClusterManagerState> hostStatuses = connector.clusterStatusService().getExtendedHostStatuses();
                    LOGGER.debug("Cluster '{}' state check, cm running, hoststates: {}", stack.getId(), hostStatuses);
                    reportHealthAndSyncInstances(stack, runningInstances, getFailedInstancesInstanceMetadata(stack, hostStatuses, runningInstances),
                            getNewHealthyHostNames(stack, hostStatuses, runningInstances), InstanceSyncState.RUNNING);
                } else {
                    syncInstances(stack, runningInstances, InstanceSyncState.DELETED_ON_PROVIDER_SIDE);
                }
            } catch (RuntimeException e) {
                syncInstances(stack, runningInstances, InstanceSyncState.DELETED_ON_PROVIDER_SIDE);
            }
        } catch (Exception e) {
            LOGGER.info("Exception during cluster state check.", e);
        } finally {
            MDCBuilder.cleanupMdc();
        }
    }

    private void buildMdcContext(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        MDCBuilder.addRequestId(UUID.randomUUID().toString());
    }

    private void reportHealthAndSyncInstances(Stack stack, Collection<InstanceMetaData> runningInstances, Collection<InstanceMetaData> failedInstances,
            Set<String> newHealtyHostNames, InstanceSyncState defaultState) {
        Set<String> failedNodeNames = failedInstances.stream()
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(toSet());
        clusterFlowService.reportHealthChange(stack.getResourceCrn(), failedNodeNames, newHealtyHostNames);
        if (failedNodeNames.size() > 0) {
            clusterService.updateClusterStatusByStackId(stack.getId(), Status.AMBIGUOUS);
        } else if (EnumSet.of(Status.AMBIGUOUS, Status.STOPPED).contains(stack.getCluster().getStatus())) {
            clusterService.updateClusterStatusByStackId(stack.getId(), Status.AVAILABLE);
        }
        syncInstances(stack, runningInstances, failedInstances, defaultState);
    }

    private boolean isClusterManagerRunning(Stack stack, ClusterApi connector) {
        return !stack.isStopped()
                && !stack.isStackInDeletionOrFailedPhase()
                && !queryClusterStatus(connector).getClusterStatus().equals(ClusterStatus.AMBARISERVER_NOT_RUNNING);
    }

    private void syncInstances(Stack stack, Collection<InstanceMetaData> instanceMetaData, InstanceSyncState defaultState) {
        syncInstances(stack, instanceMetaData, instanceMetaData, defaultState);
    }

    private void syncInstances(Stack stack, Collection<InstanceMetaData> runningInstances,
            Collection<InstanceMetaData> instanceMetaData, InstanceSyncState defaultState) {
        List<CloudVmInstanceStatus> instanceStatuses = queryInstanceStatuses(stack, instanceMetaData);
        LOGGER.debug("Cluster '{}' state check on provider, instances: {}", stack.getId(), instanceStatuses);
        syncService.autoSync(stack, runningInstances, instanceStatuses, true, defaultState);
    }

    private ClusterStatusResult queryClusterStatus(ClusterApi connector) {
        return connector.clusterStatusService().getStatus(false);
    }

    private Set<String> getNewHealthyHostNames(Stack stack, Map<HostName, ClusterManagerState> hostStatuses, Set<InstanceMetaData> runningInstances) {
        Set<String> healthyHosts = hostStatuses.entrySet().stream()
                .filter(e -> e.getValue().getClusterManagerStatus() == ClusterManagerState.ClusterManagerStatus.HEALTHY)
                .map(Map.Entry::getKey)
                .map(HostName::value)
                .collect(Collectors.toSet());
        Set<String> unhealthyStoredHosts = runningInstances.stream()
                .filter(i -> i.getInstanceStatus() == SERVICES_UNHEALTHY || i.getInstanceStatus() == SERVICES_RUNNING)
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(Collectors.toSet());
        return Sets.intersect(healthyHosts, unhealthyStoredHosts);
    }

    private Set<InstanceMetaData> getFailedInstancesInstanceMetadata(Stack stack, Map<HostName, ClusterManagerState> hostStatuses,
            Set<InstanceMetaData> runningInstances) {
        Set<String> failedHosts = hostStatuses.entrySet().stream()
                .filter(e -> e.getValue().getClusterManagerStatus() == ClusterManagerState.ClusterManagerStatus.UNHEALTHY)
                .map(Map.Entry::getKey)
                .map(HostName::value)
                .collect(Collectors.toSet());
        Set<String> noReportHosts = runningInstances.stream()
                .filter(i -> hostStatuses.get(hostName(i.getDiscoveryFQDN())) == null)
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(Collectors.toSet());
        return runningInstances.stream()
                .filter(i -> failedHosts.contains(i.getDiscoveryFQDN()) || noReportHosts.contains(i.getDiscoveryFQDN()))
                .collect(Collectors.toSet());
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

    private List<CloudVmInstanceStatus> queryInstanceStatuses(Stack stack, Collection<InstanceMetaData> instanceMetaData) {
        List<CloudVmInstanceStatus> result = Collections.emptyList();
        if (instanceMetaData.size() > 0) {
            List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(instanceMetaData);
            cloudInstances.forEach(instance -> stack.getParameters().forEach(instance::putParameter));
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
            CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getPlatformVariant(),
                    location, stack.getCreator().getUserId(), stack.getWorkspace().getId());
            CloudCredential cloudCredential = getCloudCredential(stack.getEnvironmentCrn());
            result = getCloudVmInstanceStatuses(cloudInstances, cloudContext, cloudCredential);
        }
        return result;
    }

    private List<CloudVmInstanceStatus> getCloudVmInstanceStatuses(List<CloudInstance> cloudInstances,
            CloudContext cloudContext, CloudCredential cloudCredential) {
        List<CloudVmInstanceStatus> instanceStatuses = Collections.emptyList();
        try {
            instanceStatuses = instanceStateQuery.getCloudVmInstanceStatuses(cloudCredential, cloudContext, cloudInstances);
        } catch (RuntimeException e) {
            instanceStatuses = cloudInstances.stream()
                    .map(instance -> new CloudVmInstanceStatus(instance, InstanceStatus.UNKNOWN))
                    .collect(Collectors.toList());
        }
        return instanceStatuses;
    }

    private CloudCredential getCloudCredential(String environmentCrn) {
        return cloudCredentialConverter.convert(
                credentialConverter.convert(
                        environmentInternalCrnClient.withInternalCrn().credentialV1Endpoint().getByEnvironmentCrn(environmentCrn)
                )
        );
    }
}

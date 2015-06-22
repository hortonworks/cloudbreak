package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.domain.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.domain.Status.STOPPED;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.flow.service.AmbariHostsRemover;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;

@Service
public class StackSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackSyncService.class);

    @Inject
    private StackService stackService;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Inject
    private InstanceGroupRepository instanceGroupRepository;
    @Inject
    private ResourceRepository resourceRepository;
    @Inject
    private HostGroupRepository hostGroupRepository;
    @Inject
    private HostMetadataRepository hostMetadataRepository;
    @Inject
    private AmbariClusterConnector ambariClusterConnector;
    @Inject
    private AmbariClientProvider ambariClientProvider;
    @Inject
    private TlsSecurityService tlsSecurityService;
    @Inject
    private AmbariHostsRemover ambariHostsRemover;
    @javax.annotation.Resource
    private Map<CloudPlatform, MetadataSetup> metadataSetups;

    public void sync(Long stackId) throws Exception {
        Stack stack = stackService.getById(stackId);
        Set<InstanceMetaData> instanceMetaDatas = instanceMetaDataRepository.findAllInStack(stackId);
        String statusReason = "State of the cluster infrastructure has been synchronized.";
        for (InstanceMetaData instanceMetaData : instanceMetaDatas) {
            InstanceGroup instanceGroup = instanceMetaData.getInstanceGroup();
            InstanceSyncState state = metadataSetups.get(stack.cloudPlatform()).getState(stack, instanceMetaData.getInstanceId());
            if (InstanceSyncState.DELETED.equals(state) && !instanceMetaData.isTerminated()) {
                updateMetadataToTerminatedAndDeregisterFromAmbari(stack, stack.getCluster(), instanceMetaData, instanceGroup);
            } else if (InstanceSyncState.RUNNING.equals(state) && !instanceMetaData.isRegistered()) {
                updateMetaDataToRunning(stackId, stack.getCluster(), instanceMetaData, instanceGroup);
            } else if (InstanceSyncState.STOPPED.equals(state) && !instanceMetaData.isTerminated()) {
                updateMetaDataToTerminated(stackId, instanceMetaData, instanceGroup);
            }
        }
        if (Status.stopStatusesForUpdate().contains(stack.getStatus())) {
            stackUpdater.updateStackStatus(stack.getId(), STOPPED, statusReason);
        } else if (Status.availableStatusesForUpdate().contains(stack.getStatus())) {
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, statusReason);
        } else {
            stackUpdater.updateStackStatus(stack.getId(), stack.getStatus(), statusReason);
        }
    }

    private void updateMetadataToTerminatedAndDeregisterFromAmbari(Stack stack, Cluster cluster, InstanceMetaData instanceMetaData,
            InstanceGroup instanceGroup) {
        instanceGroup.setNodeCount(instanceGroup.getNodeCount() - 1);
        instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
        boolean deregisterSuccess = false;
        try {
            if (cluster != null) {
                HostGroup hostGroup = hostGroupRepository.findHostGroupsByInstanceGroupName(cluster.getId(), instanceGroup.getGroupName());
                HostMetadata data = hostMetadataRepository.findHostsInClusterByName(cluster.getId(), instanceMetaData.getDiscoveryFQDN());
                deregisterFromAmbari(stack, cluster, hostGroup, data);
            }
            deregisterSuccess = true;
        } catch (Exception ex) {
            LOGGER.error("Terminated instance deregistration from ambari was unsuccess: ", ex);
        }
        if (deregisterSuccess) {
            instanceMetaDataRepository.save(instanceMetaData);
            instanceGroupRepository.save(instanceGroup);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    String.format("Instance %s was terminated not by us update metadata...", instanceMetaData.getInstanceId()));
        }
    }

    private void updateMetaDataToTerminated(Long stackId, InstanceMetaData instanceMetaData, InstanceGroup instanceGroup) {
        instanceGroup.setNodeCount(instanceGroup.getNodeCount() - 1);
        instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceMetaDataRepository.save(instanceMetaData);
        instanceGroupRepository.save(instanceGroup);
        eventService.fireCloudbreakEvent(stackId, AVAILABLE.name(),
                String.format("Instance %s was stopped by hand update metadata...", instanceMetaData.getInstanceId()));
    }

    private void updateMetaDataToRunning(Long stackId, Cluster cluster, InstanceMetaData instanceMetaData, InstanceGroup instanceGroup) {
        instanceGroup.setNodeCount(instanceGroup.getNodeCount() + 1);
        HostMetadata data = null;
        try {
            if (cluster != null) {
                data = hostMetadataRepository.findHostsInClusterByName(cluster.getId(), instanceMetaData.getDiscoveryFQDN());
            }
        } catch (Exception ex) {
            LOGGER.warn("This {} instance was not added to Ambari.", instanceMetaData.getInstanceId());
        }
        if (data == null) {
            instanceMetaData.setInstanceStatus(InstanceStatus.UNREGISTERED);
        } else {
            instanceMetaData.setInstanceStatus(InstanceStatus.REGISTERED);
        }
        instanceMetaDataRepository.save(instanceMetaData);
        instanceGroupRepository.save(instanceGroup);
        eventService.fireCloudbreakEvent(stackId, AVAILABLE.name(),
                String.format("Instance %s was restarted by hand update metadata...", instanceMetaData.getInstanceId()));
    }

    private void deregisterFromAmbari(Stack stack, Cluster cluster, HostGroup hostGroup, HostMetadata data) throws CloudbreakSecuritySetupException {
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), stack.getCluster().getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getSecureAmbariClient(clientConfig, stack.getCluster());
        Set<String> components = ambariClusterConnector.getHadoopComponents(cluster, ambariClient, hostGroup.getName(),
                stack.getCluster().getBlueprint().getBlueprintName());
        ambariHostsRemover.deleteHosts(stack, Arrays.asList(data.getHostName()), new ArrayList<>(components));
        PollingResult pollingResult = ambariClusterConnector.restartHadoopServices(stack, ambariClient, true);
        if (isSuccess(pollingResult)) {
            hostMetadataRepository.delete(data);
        }
    }
}

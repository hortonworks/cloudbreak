package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterContainerRunner;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleHostContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.UpscaleClusterFailedPayload;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ClusterUpscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterContainerRunner containerRunner;

    @Inject
    private AmbariClusterConnector ambariClusterConnector;

    @Inject
    private InstanceMetadataService instanceMetadataService;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private EmailSenderService emailSenderService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    public void addClusterContainers(ClusterUpscaleContext context, ClusterScalingContext payload) throws CloudbreakException {
        LOGGER.info("Start adding cluster containers");
        Stack stack = context.getStack();
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS, "Adding new containers to the cluster.");
        Map<String, List<Container>> containers = containerRunner.addClusterContainers(payload);
        Map<String, List<String>> hostsPerHostGroup = new HashMap<>();
        for (Map.Entry<String, List<Container>> containersEntry : containers.entrySet()) {
            List<String> hostNames = new ArrayList<>();
            for (Container container : containersEntry.getValue()) {
                hostNames.add(container.getHost());
            }
            hostsPerHostGroup.put(containersEntry.getKey(), hostNames);
        }
        clusterService.updateHostMetadata(stack.getCluster().getId(), hostsPerHostGroup);
        Set<String> allHosts = new HashSet<>();
        for (Map.Entry<String, List<String>> hostsPerHostGroupEntry : hostsPerHostGroup.entrySet()) {
            allHosts.addAll(hostsPerHostGroupEntry.getValue());
        }
        clusterService.updateHostCountWithAdjustment(stack.getCluster().getId(), payload.getHostGroupAdjustment().getHostGroup(), allHosts.size());
        instanceMetadataService.updateInstanceStatus(stack.getInstanceGroups(), InstanceStatus.UNREGISTERED, allHosts);
        clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE, "New containers added to the cluster.");
    }

    public Set<HostMetadata> upscaleCluster(ClusterUpscaleContext context, ClusterScalingContext payload) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start upscale cluster");
        Stack stack = context.getStack();
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALING_UP, UPDATE_IN_PROGRESS.name());
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), payload.getHostGroupAdjustment().getHostGroup());
        ambariClusterConnector.prepareAmbariNodes(stack, hostGroup);
        return hostMetadataRepository.findHostsInCluster(stack.getCluster().getId());
    }

    public void configureSssd(ClusterUpscaleContext context, PollingService.Callback callback) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start configuring SSSD");
        Stack stack = context.getStack();
        ambariClusterConnector.configureSssd(stack, callback);
    }

    public void installRecipes(ClusterUpscaleHostContext context, PollingService.Callback callback) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start installing recipes");
        Stack stack = context.getStack();
        ambariClusterConnector.installRecipes(stack, context.getHostGroup(), context.getHostMetadata(), callback);
    }

    public void executePreRecipes(ClusterUpscaleHostContext context, PollingService.Callback callback) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start executing pre recipes");
        Stack stack = context.getStack();
        ambariClusterConnector.executePreRecipes(stack, context.getHostGroup(), context.getHostMetadata(), callback);
    }

    public void installServices(ClusterUpscaleHostContext context, PollingService.Callback callback) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start installing Ambari services");
        Stack stack = context.getStack();
        ambariClusterConnector.installServices(stack, context.getHostGroup(), context.getHostMetadata(), callback);
    }

    public void executePostRecipes(ClusterUpscaleHostContext context, PollingService.Callback callback) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start executing post recipes");
        Stack stack = context.getStack();
        ambariClusterConnector.executePostRecipes(stack, context.getHostGroup(), context.getHostMetadata(), callback);
    }

    public void finalizeUpscale(ClusterUpscaleHostContext context, ClusterScalingContext payload) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start finalize upscale");
        Stack stack = context.getStack();
        ambariClusterConnector.updateFailedHostMetaData(context.getHostMetadata());
        int failedHosts = 0;
        for (HostMetadata hostMeta : context.getHostMetadata()) {
            if (!"BYOS".equals(stack.cloudPlatform()) && context.getHostGroup().getConstraint().getInstanceGroup() != null) {
                stackService.updateMetaDataStatus(stack.getId(), hostMeta.getHostName(), InstanceStatus.REGISTERED);
            }
            hostGroupService.updateHostMetaDataStatus(hostMeta.getId(), HostMetadataState.HEALTHY);
            if (hostMeta.getHostMetadataState() == HostMetadataState.UNHEALTHY) {
                failedHosts++;
            }
        }
        boolean success = failedHosts == 0;
        if (success) {
            LOGGER.info("Cluster upscaled successfully");
            clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
            flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALED_UP, AVAILABLE.name());
            if (stack.getCluster().getEmailNeeded()) {
                emailSenderService.sendUpscaleSuccessEmail(stack.getCluster().getOwner(), stack.getAmbariIp(), stack.getCluster().getName());
                flowMessageService.fireEventAndLog(payload.getStackId(), Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, AVAILABLE.name());
            }
        } else {
            LOGGER.info("Cluster upscale failed. {} hosts failed to upscale", failedHosts);
            clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_FAILED);
            flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "added",
                    String.format("Ambari upscale operation failed on %d node(s).", failedHosts));
        }
    }

    public void handleFailure(ClusterUpscaleContext context, UpscaleClusterFailedPayload payload) {
        Stack stack = context.getStack();
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_FAILED, payload.getErrorReason());
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, String.format("New node(s) could not be added to the cluster: %s", payload.getErrorReason()));
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "added", payload.getErrorReason());
    }
}

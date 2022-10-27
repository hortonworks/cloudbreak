package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;
import static com.sequenceiq.periscope.api.model.ClusterState.SUSPENDED;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.DependentHostGroupsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.monitor.event.ClusterStatusSyncEvent;
import com.sequenceiq.periscope.service.AltusMachineUserService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.DependentHostGroupsService;
import com.sequenceiq.periscope.utils.LoggingUtils;

@Component
public class ClusterStatusSyncHandler implements ApplicationListener<ClusterStatusSyncEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStatusSyncHandler.class);

    // TODO: Better way to define such constants?
    private static final String UNDEFINED_DEPENDENCY = "UNDEFINED_DEPENDENCY";

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Inject
    private DependentHostGroupsService dependentHostGroupsService;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Override
    public void onApplicationEvent(ClusterStatusSyncEvent event) {
        long autoscaleClusterId = event.getClusterId();
        Cluster cluster = clusterService.findById(autoscaleClusterId);
        if (cluster == null) {
            return;
        }
        LoggingUtils.buildMdcContext(cluster);

        StackV4Response stackResponse = cloudbreakCommunicator.getByCrn(cluster.getStackCrn());
        DependentHostGroupsV4Response dependentHostGroupsResponse = dependentHostGroupsService.getDependentHostGroupsForPolicyHostGroup(cluster.getStackCrn(),
                extractPolicyHostGroup(cluster));

        boolean clusterAvailable = determineClusterAvailability(stackResponse, dependentHostGroupsResponse);

        LOGGER.info("Computed clusterAvailable: {}", clusterAvailable);
        LOGGER.info("Analysing CBCluster Status '{}' for Cluster '{}. Available(Determined)={}' ", stackResponse, cluster.getStackCrn(), clusterAvailable);

        updateClusterState(cluster, stackResponse, clusterAvailable);
    }

    private void updateClusterState(Cluster cluster, StackV4Response stackResponse, boolean clusterAvailable) {
        if (DELETE_COMPLETED.equals(stackResponse.getStatus())) {
            beforeDeleteCleanup(cluster);
            clusterService.removeById(cluster.getId());
            LOGGER.info("Deleted cluster '{}', CB Stack Status '{}'.", cluster.getStackCrn(), stackResponse.getStatus());
        } else if (clusterAvailable && !RUNNING.equals(cluster.getState())) {
            clusterService.setState(cluster.getId(), ClusterState.RUNNING);
            LOGGER.info("Updated cluster '{}' to Running, CB Stack Status '{}', CB Cluster Status '{}'.",
                    cluster.getStackCrn(), stackResponse.getStatus(), stackResponse.getCluster().getStatus());
        } else if (!clusterAvailable && RUNNING.equals(cluster.getState())) {
            clusterService.setState(cluster.getId(), SUSPENDED);
            LOGGER.info("Suspended cluster '{}', CB Stack Status '{}', CB Cluster Status '{}'", cluster.getStackCrn(), stackResponse.getStatus(),
                    stackResponse.getCluster().getStatus());
        } else if (RUNNING.equals(cluster.getState()) && (cluster.getMachineUserCrn() == null || cluster.getEnvironmentCrn() == null)) {
            populateEnvironmentAndMachineUserIfNotPresent(cluster);
        }
    }

    private boolean determineClusterAvailability(StackV4Response stackResponse, DependentHostGroupsV4Response dependentHostGroupsResponse) {
        Set<String> dependentHostGroups = dependentHostGroupsResponse.getDependentHostGroups();
        if (dependentHostGroups.contains(UNDEFINED_DEPENDENCY)) {
            return stackResponse.getStatus().isAvailable();
        }
        return !(stackDeletionInProgress(stackResponse) || stackModificationInProgress(stackResponse)
                || dependentHostsUnHealthy(dependentHostGroupsResponse, stackResponse));
    }

    private boolean dependentHostsUnHealthy(DependentHostGroupsV4Response dependentHostGroupsResponse, StackV4Response stackResponse) {
        return stackResponse.getInstanceGroups().stream()
                .flatMap(ig -> ig.getMetadata().stream())
                .filter(im -> dependentHostGroupsResponse.getDependentHostGroups().contains(im.getInstanceGroup()))
                .anyMatch(im -> !InstanceStatus.SERVICES_HEALTHY.equals(im.getInstanceStatus()));
    }

    private boolean stackModificationInProgress(StackV4Response stack) {
        return stack.getStatus().isInProgress();
    }

    private boolean stackDeletionInProgress(StackV4Response stack) {
        return stack.getStatus().isTerminatedOrDeletionInProgress();
    }

    // TODO: Only one policyHostGroup?
    private String extractPolicyHostGroup(Cluster cluster) {
        Set<TimeAlert> timeAlerts = cluster.getTimeAlerts();
        Set<LoadAlert> loadAlerts = cluster.getLoadAlerts();

        if (!timeAlerts.isEmpty()) {
            return timeAlerts.stream().iterator().next().getScalingPolicy().getHostGroup();
        }
        return loadAlerts.stream().iterator().next().getScalingPolicy().getHostGroup();
    }

    protected void beforeDeleteCleanup(Cluster cluster) {
        try {
            if ((cluster.getEnvironmentCrn() != null) && (clusterService.countByEnvironmentCrn(cluster.getEnvironmentCrn()) <= 1)) {
                altusMachineUserService.deleteMachineUserForEnvironment(cluster.getClusterPertain().getTenant(),
                        cluster.getMachineUserCrn(), cluster.getEnvironmentCrn());
            }
        } catch (Exception ex) {
            LOGGER.warn("Error deleting machineUserCrn '{}' for environment '{}'",
                    cluster.getMachineUserCrn(), cluster.getEnvironmentCrn(), ex);
        }
    }

    protected void populateEnvironmentAndMachineUserIfNotPresent(Cluster cluster) {
        if (cluster.getEnvironmentCrn() == null) {
            String envCrn = cloudbreakCommunicator.getAutoscaleClusterByCrn(cluster.getStackCrn()).getEnvironmentCrn();
            clusterService.setEnvironmentCrn(cluster.getId(), envCrn);
        }

        if (cluster.getMachineUserCrn() == null) {
            altusMachineUserService.initializeMachineUserForEnvironment(cluster);
        }
    }
}

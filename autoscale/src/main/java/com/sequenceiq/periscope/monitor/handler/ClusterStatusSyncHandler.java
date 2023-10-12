package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;
import static com.sequenceiq.periscope.api.model.ClusterState.SUSPENDED;
import static java.util.stream.Collectors.toSet;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.DependentHostGroupsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.monitor.event.ClusterStatusSyncEvent;
import com.sequenceiq.periscope.service.AltusMachineUserService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.DependentHostGroupsService;
import com.sequenceiq.periscope.utils.LoggingUtils;
import com.sequenceiq.periscope.utils.StackResponseUtils;

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
    private StackResponseUtils stackResponseUtils;

    @Inject
    private ClouderaManagerCommunicator cmCommunicator;

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
        if (stackResponse.getCluster() != null && stackResponse.getCluster().getBlueprint() != null
                && (StringUtils.isEmpty(cluster.getBluePrintText())
                || !Objects.equals(cluster.getBluePrintText(), stackResponse.getCluster().getBlueprint().getBlueprint()))) {
            cluster.setBluePrintText(stackResponse.getCluster().getBlueprint().getBlueprint());
            clusterService.save(cluster);
        }

        boolean clusterAvailable = determineClusterAvailability(cluster, stackResponse) && determineCmAvailability(cluster, stackResponse);

        LOGGER.info("Computed clusterAvailable: {}", clusterAvailable);
        LOGGER.info("Analysing CBCluster Status '{}' for Cluster '{}. Available(Determined)={}' ", stackResponse, cluster.getStackCrn(), clusterAvailable);

        updateClusterState(cluster, stackResponse, clusterAvailable);
    }

    private boolean determineCmAvailability(Cluster cluster, StackV4Response stackResponse) {
        return stackResponseUtils.primaryGatewayHealthy(stackResponse) && cmCommunicator.isClusterManagerRunning(cluster);
    }

    private void updateClusterState(Cluster cluster, StackV4Response stackResponse, boolean clusterAvailable) {
        if (clusterAvailable && !RUNNING.equals(cluster.getState())) {
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

    private boolean determineClusterAvailability(Cluster cluster, StackV4Response stackResponse) {
        if (!Boolean.TRUE.equals(cluster.isStopStartScalingEnabled())) {
            return Optional.ofNullable(stackResponse.getStatus()).map(Status::isAvailable).orElse(false);
        } else {
            Set<String> policyHostGroups = extractPolicyHostGroups(cluster);
            DependentHostGroupsV4Response dependentHostGroupsResponse =
                    dependentHostGroupsService.getDependentHostGroupsForPolicyHostGroups(cluster.getStackCrn(), policyHostGroups);

            if (undefinedDependencyPresent(dependentHostGroupsResponse, policyHostGroups)) {
                return Optional.ofNullable(stackResponse.getStatus()).map(Status::isAvailable).orElse(false);
            }

            boolean dependentHostsUnhealthy = policyHostGroups.stream().anyMatch(hg -> dependentHostsUnhealthy(dependentHostGroupsResponse, stackResponse, hg));

            return !(stackDeletionInProgress(stackResponse) || stackStatusInProgress(stackResponse) || stackStopped(stackResponse)
                    || dependentHostsUnhealthy);
        }
    }

    private boolean dependentHostsUnhealthy(DependentHostGroupsV4Response dependentHostGroupsResponse, StackV4Response stackResponse, String policyHostGroup) {
        Set<String> unhealthyDependentHosts = stackResponseUtils.getUnhealthyDependentHosts(stackResponse, dependentHostGroupsResponse, policyHostGroup);
        logUnhealthyHostNamesIfPresent(unhealthyDependentHosts, policyHostGroup);
        return !unhealthyDependentHosts.isEmpty();
    }

    private void logUnhealthyHostNamesIfPresent(Set<String> unhealthyDependentHosts, String hostGroup) {
        if (unhealthyDependentHosts.isEmpty()) {
            LOGGER.info("No unhealthy dependent hosts for hostGroup: {}", hostGroup);
        } else {
            LOGGER.info("Detected unhealthy dependent hosts: {} for hostGroup: {}", unhealthyDependentHosts, hostGroup);
        }
    }

    private boolean undefinedDependencyPresent(DependentHostGroupsV4Response dependentHostGroupsResponse, Set<String> policyHostgroups) {
        return policyHostgroups
                .stream()
                .anyMatch(hg -> dependentHostGroupsResponse.getDependentHostGroups().getOrDefault(hg, Set.of()).contains(UNDEFINED_DEPENDENCY));
    }

    private boolean stackStatusInProgress(StackV4Response stack) {
        return Optional.ofNullable(stack.getStatus()).map(Status::isInProgress).orElse(false);
    }

    private boolean stackDeletionInProgress(StackV4Response stack) {
        return Optional.ofNullable(stack.getStatus()).map(Status::isTerminatedOrDeletionInProgress).orElse(false);
    }

    private boolean stackStopped(StackV4Response stack) {
        return Optional.ofNullable(stack.getStatus()).map(Status::isStopState).orElse(false);
    }

    private boolean isPolicyAttached(BaseAlert baseAlert) {
        return Objects.nonNull(baseAlert.getScalingPolicy());
    }

    private Set<String> extractPolicyHostGroups(Cluster cluster) {
        Set<TimeAlert> timeAlerts = cluster.getTimeAlerts();
        Set<LoadAlert> loadAlerts = cluster.getLoadAlerts();

        if (!timeAlerts.isEmpty()) {
            return timeAlerts.stream().filter(this::isPolicyAttached).map(sbp -> sbp.getScalingPolicy().getHostGroup()).collect(toSet());
        }
        return loadAlerts.stream().filter(this::isPolicyAttached).map(lbp -> lbp.getScalingPolicy().getHostGroup()).collect(toSet());
    }

    protected void beforeDeleteCleanup(Cluster cluster) {
        try {
            if (cluster.getEnvironmentCrn() != null && clusterService.countByEnvironmentCrn(cluster.getEnvironmentCrn()) <= 1) {
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

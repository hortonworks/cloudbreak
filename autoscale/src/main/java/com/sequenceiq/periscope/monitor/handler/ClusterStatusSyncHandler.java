package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;
import static com.sequenceiq.periscope.api.model.ClusterState.SUSPENDED;
import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.DependentHostGroupsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
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

    private static final String GOOD_HEALTH = "GOOD";

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
        if (!cluster.isAutoscalingEnabled()) {
            LOGGER.info("Cannot assess autoscaling state as Autoscaling is disabled for: {}", cluster.getStackCrn());
            return;
        }
        LoggingUtils.buildMdcContext(cluster);

        StackV4Response stackResponse = cloudbreakCommunicator.getByCrn(cluster.getStackCrn());
        if (stackResponse.getCluster() != null && stackResponse.getCluster().getExtendedBlueprintText() != null
                && (StringUtils.isEmpty(cluster.getBluePrintText())
                || !Objects.equals(cluster.getBluePrintText(), stackResponse.getCluster().getExtendedBlueprintText()))) {
            cluster.setBluePrintText(stackResponse.getCluster().getExtendedBlueprintText());
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
            clusterService.setState(cluster.getId(), RUNNING);
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

            boolean dependentHostsUnhealthy = policyHostGroups.stream().anyMatch(hg ->
                    dependentHostsUnhealthy(dependentHostGroupsResponse, stackResponse, hg, cluster));

            return !(stackDeletionInProgress(stackResponse) || stackStatusInProgress(stackResponse) || stackStopped(stackResponse)
                    || dependentHostsUnhealthy);
        }
    }

    private boolean dependentHostsUnhealthy(DependentHostGroupsV4Response dependentHostGroupsResponse, StackV4Response stackResponse,
            String policyHostGroup, Cluster cluster) {
        Set<String> dependentComponents = dependentHostGroupsResponse.getDependentComponents().getOrDefault(policyHostGroup, Set.of());
        Set<String> unhealthyDependentHosts = stackResponseUtils.getUnhealthyDependentHosts(stackResponse, dependentHostGroupsResponse, policyHostGroup);
        logUnhealthyHostNamesIfPresent(unhealthyDependentHosts, policyHostGroup);
        if (!unhealthyDependentHosts.isEmpty()) {
            Map<String, String> hostServicesHealth = cmCommunicator.readServicesHealth(cluster);
            Set<String> unhealthyComponents = new HashSet<>();
            for (String component : dependentComponents) {
                String healthCheck = String.valueOf(hostServicesHealth
                        .entrySet()
                        .stream()
                        .filter(i -> i.getKey().contains(component))
                        .map(Map.Entry::getValue)
                        .findFirst().get());
                if (!Objects.equals(healthCheck, GOOD_HEALTH)) {
                    unhealthyComponents.add(component);
                }
            }
            logUnhealthyDependentComponents(unhealthyComponents, policyHostGroup);
            return !unhealthyComponents.isEmpty();
        }
        return false;
    }

    private void logUnhealthyDependentComponents(Set<String> unhealthyDependentComponents, String hostGroup) {
        if (unhealthyDependentComponents.isEmpty()) {
            LOGGER.info("No unhealthy dependent components for hostGroup: {}", hostGroup);
        } else {
            LOGGER.info("Detected unhealthy dependent components: {} for hostGroup: {}", unhealthyDependentComponents, hostGroup);
        }
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

package com.sequenceiq.periscope.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;
import static com.sequenceiq.periscope.api.model.ClusterState.SUSPENDED;
import static com.sequenceiq.periscope.common.MessageCode.CLUSTER_EXISTS_FOR_CRN;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.ScalingConfigurationRequest;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.MetricType;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.domain.UpdateFailedDetails;
import com.sequenceiq.periscope.model.MonitoredStack;
import com.sequenceiq.periscope.repository.ClusterPertainRepository;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.SecurityConfigRepository;
import com.sequenceiq.periscope.service.security.SecurityConfigService;
import com.sequenceiq.periscope.utils.LoggingUtils;

@Service
public class ClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterService.class);

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private ClusterPertainRepository clusterPertainRepository;

    @Inject
    private NodeConfig periscopeNodeConfig;

    @Inject
    private PeriscopeMetricService periscopeMetricService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ScalingActivityService scalingActivityService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @PostConstruct
    protected void init() {
        calculateClusterStateMetrics();
    }

    public Cluster create(AutoscaleStackV4Response stack) {
        Cluster cluster = new Cluster();
        cluster.setStackName(stack.getName());
        cluster.setBluePrintText(stack.getBluePrintText());
        cluster.setStackCrn(stack.getStackCrn());
        cluster.setStackId(stack.getStackId());
        cluster.setStackType(stack.getStackType());
        cluster.setTunnel(stack.getTunnel());
        cluster.setState(ClusterState.PENDING);
        cluster.setEnvironmentCrn(stack.getEnvironmentCrn());

        if (stack.getCloudPlatform() != null) {
            cluster.setCloudPlatform(stack.getCloudPlatform().toUpperCase(Locale.ROOT));
        }

        String gatewayPort = String.valueOf(stack.getGatewayPort());
        ClusterManager clusterManager =
                new ClusterManager(stack.getClusterManagerIp(), gatewayPort, stack.getUserNamePath(),
                        stack.getPasswordPath(), ClusterManagerVariant.CLOUDERA_MANAGER);
        cluster.setClusterManager(clusterManager);

        ClusterPertain clusterPertain =
                new ClusterPertain(stack.getTenant(), stack.getWorkspaceId(), stack.getUserId(), stack.getUserCrn());
        cluster.setClusterPertain(
                clusterPertainRepository.findFirstByUserCrn(clusterPertain.getUserCrn())
                        .orElseGet(() -> clusterPertainRepository.save(clusterPertain)));

        cluster = save(cluster);
        securityConfigService.syncSecurityConfigForCluster(cluster.getId());
        altusMachineUserService.initializeMachineUserForEnvironment(cluster);
        calculateClusterStateMetrics();
        return cluster;
    }

    public Cluster update(Long clusterId, MonitoredStack stack, boolean enableAutoscaling) {
        return update(clusterId, stack, null, enableAutoscaling);
    }

    public Cluster update(Long clusterId, MonitoredStack stack, ClusterState clusterState, boolean enableAutoscaling) {
        Cluster cluster = findById(clusterId);
        ClusterState newState = clusterState != null ? clusterState : cluster.getState();
        cluster.setState(newState);
        cluster.setAutoscalingEnabled(enableAutoscaling);
        cluster.setStackName(stack.getStackName());
        cluster.update(stack);
        SecurityConfig sSecConf = stack.getSecurityConfig();
        if (sSecConf != null) {
            SecurityConfig updatedConfig = sSecConf;
            SecurityConfig securityConfig = securityConfigRepository.findByClusterId(clusterId);
            if (securityConfig != null) {
                securityConfig.update(updatedConfig);
                securityConfigRepository.save(securityConfig);
            } else {
                SecurityConfig sc = new SecurityConfig(sSecConf.getClientKey(), sSecConf.getClientCert(), sSecConf.getServerCert());
                sc.setCluster(cluster);
                sc = securityConfigRepository.save(sc);
                cluster.setSecurityConfig(sc);
            }
        }
        cluster = save(cluster);
        calculateClusterStateMetrics();
        return cluster;
    }

    public List<Cluster> findDistroXByTenant(String tenant) {
        return clusterRepository.findByTenantAndStackType(tenant, StackType.WORKLOAD);
    }

    public Cluster findOneByStackId(Long stackId) {
        return clusterRepository.findByStackId(stackId);
    }

    public Optional<Cluster> findOneByStackCrnAndTenant(String stackCrn, String tenant) {
        LoggingUtils.buildMdcContextWithCrn(stackCrn);
        return clusterRepository.findByStackCrnAndTenant(stackCrn, tenant);
    }

    public Optional<Cluster> findOneByStackCrn(String stackCrn) {
        LoggingUtils.buildMdcContextWithCrn(stackCrn);
        return clusterRepository.findByStackCrn(stackCrn);
    }

    public List<Cluster> findByDeleteRetryCount(int maxDeleteRetryCount) {
        return clusterRepository.findByDeleteRetryCount(maxDeleteRetryCount);
    }

    public Optional<Cluster> findOneByStackNameAndTenant(String stackName, String tenant) {
        LoggingUtils.buildMdcContextWithName(stackName);
        return clusterRepository.findByStackNameAndTenant(stackName, tenant);
    }

    public Cluster save(Cluster cluster) {
        return clusterRepository.save(cluster);
    }

    public Cluster findById(Long clusterId) {
        Cluster cluster = clusterRepository.findById(clusterId).orElseThrow(notFound("Cluster", clusterId));
        LoggingUtils.buildMdcContext(cluster);
        return cluster;
    }

    public void removeById(Long clusterId) {
        Cluster cluster = findById(clusterId);
        LoggingUtils.buildMdcContext(cluster);
        clearScalingActivityForCluster(clusterId);
        clusterRepository.delete(cluster);
        calculateClusterStateMetrics();
    }

    private void clearScalingActivityForCluster(Long clusterId) {
        scalingActivityService.deleteScalingActivityForCluster(clusterId);
    }

    public Cluster updateScalingConfiguration(Long clusterId, ScalingConfigurationRequest scalingConfiguration) {
        Cluster cluster = findById(clusterId);
        cluster.setMinSize(scalingConfiguration.getMinSize());
        cluster.setMaxSize(scalingConfiguration.getMaxSize());
        cluster.setCoolDown(scalingConfiguration.getCoolDown());
        return save(cluster);
    }

    public ScalingConfigurationRequest getScalingConfiguration(Long clusterId) {
        Cluster cluster = findById(clusterId);
        ScalingConfigurationRequest configuration = new ScalingConfigurationRequest();
        configuration.setCoolDown(cluster.getCoolDown());
        configuration.setMaxSize(cluster.getMaxSize());
        configuration.setMinSize(cluster.getMinSize());
        return configuration;
    }

    public Cluster setState(Long clusterId, ClusterState state) {
        Cluster cluster = findById(clusterId);
        LoggingUtils.buildMdcContext(cluster);
        cluster.setState(state);
        calculateClusterStateMetrics();
        return clusterRepository.save(cluster);
    }

    public Cluster setAutoscaleState(Long clusterId, Boolean enableAutoscaling) {
        Cluster cluster = findById(clusterId);
        LoggingUtils.buildMdcContext(cluster);
        cluster.setAutoscalingEnabled(enableAutoscaling);
        cluster = clusterRepository.save(cluster);
        calculateClusterStateMetrics();
        return cluster;
    }

    public Cluster setStopStartScalingState(Cluster cluster, Boolean enableStopStartScaling) {
        LoggingUtils.buildMdcContext(cluster);
        cluster.setStopStartScalingEnabled(enableStopStartScaling);
        cluster = clusterRepository.save(cluster);
        calculateClusterStateMetrics();
        return cluster;
    }

    public void setLastEvaluated(Long clusterId, Long lastEvaluated) {
        clusterRepository.setClusterLastEvaluated(clusterId, lastEvaluated);
    }

    public void setEnvironmentCrn(Long clusterId, String environmentCrn) {
        clusterRepository.setEnvironmentCrn(clusterId, environmentCrn);
    }

    public void setMachineUserCrn(Long clusterId, String machineUserCrn) {
        clusterRepository.setMachineUserCrn(clusterId, machineUserCrn);
    }

    public void setLastScalingActivity(Long clusterId, Long lastScalingActivity) {
        clusterRepository.setClusterLastScalingActivity(clusterId, lastScalingActivity);
    }

    public void setUpdateFailedDetails(Long clusterId, UpdateFailedDetails updateFailedDetails) {
        clusterRepository.setClusterUpdateFailedDetails(clusterId, updateFailedDetails);
    }

    public List<Cluster> findAllByPeriscopeNodeId(String nodeId) {
        return clusterRepository.findAllByPeriscopeNodeId(nodeId);
    }

    public List<Cluster> findAllByStateAndNode(ClusterState state, String nodeId) {
        return clusterRepository.findByStateAndPeriscopeNodeId(state, nodeId);
    }

    public List<Cluster> findAllForNode(ClusterState state, boolean autoscalingEnabled, String nodeId) {
        return clusterRepository.findByStateAndAutoscalingEnabledAndPeriscopeNodeId(state, autoscalingEnabled, nodeId);
    }

    public List<Cluster> findClustersByClusterIds(List<Long> clusterIds) {
        return clusterRepository.findClustersByClusterIds(clusterIds);
    }

    public List<Cluster> findByEnvironmentCrnOrMachineUserCrn(String environmentCrn, String machineUserCrn) {
        return clusterRepository.findByEnvironmentCrnOrMachineUserCrn(environmentCrn, machineUserCrn);
    }

    public List<Long> findLoadAlertClusterIdsForPeriscopeNodeId(StackType stackType, ClusterState state,
            boolean autoscalingEnabled, String nodeId) {
        return clusterRepository.findByLoadAlertAndStackTypeAndClusterStateAndAutoscaling(stackType, state, autoscalingEnabled, nodeId);
    }

    public List<Long> findTimeAlertClusterIdsForPeriscopeNodeId(StackType stackType, boolean autoscalingEnabled, String nodeId) {
        return clusterRepository.findByTimeAlertAndStackTypeAndAutoscaling(stackType, autoscalingEnabled, nodeId);
    }

    public List<Long> findClusterIdsByStackTypeAndPeriscopeNodeId(StackType stackType, String nodeId) {
        return clusterRepository.findClusterIdsByStackTypeAndPeriscopeNodeId(stackType, nodeId);
    }

    public List<Long> findClusterIdsByStackTypeAndPeriscopeNodeIdAndAutoscalingEnabled(StackType stackType, String nodeId,
            Boolean autoscalingEnabled) {
        return clusterRepository.findClusterIdsByStackTypeAndPeriscopeNodeIdAndAutoscalingEnabled(stackType, nodeId, autoscalingEnabled);
    }

    public void validateClusterUniqueness(MonitoredStack stack) {
        Iterable<Cluster> clusters = clusterRepository.findAll();
        boolean clusterForTheSameStackAndClusterManager = StreamSupport.stream(clusters.spliterator(), false)
                .anyMatch(cluster -> {
                    boolean equalityOfStackCrn = cluster.getStackCrn() != null && Objects.equals(cluster.getStackCrn(), stack.getStackCrn());
                    ClusterManager clusterManager = cluster.getClusterManager();
                    ClusterManager newClusterManager = stack.getClusterManager();
                    boolean clrMgrObjectsNotNull = clusterManager != null && newClusterManager != null;
                    boolean clrMgrHostsNotEmpty = clrMgrObjectsNotNull && !isEmpty(clusterManager.getHost()) && !isEmpty(newClusterManager.getHost());
                    boolean equalityOfCMHost = clrMgrObjectsNotNull && clrMgrHostsNotEmpty && clusterManager.getHost().equals(newClusterManager.getHost());
                    return equalityOfStackCrn && equalityOfCMHost;
                });
        if (clusterForTheSameStackAndClusterManager) {
            throw new BadRequestException(
                    messagesService.getMessage(CLUSTER_EXISTS_FOR_CRN, Set.of(stack.getStackCrn(), stack.getClusterManager().getVariant().name())));
        }
    }

    public Cluster deleteAlertsForCluster(Long clusterId) {
        Cluster cluster = findById(clusterId);
        cluster.getLoadAlerts().clear();
        cluster.getTimeAlerts().clear();
        save(cluster);
        return cluster;
    }

    public Integer countByEnvironmentCrn(String environmentCrn) {
        Integer countByEnvironmentCrn = clusterRepository.countByEnvironmentCrn(environmentCrn);
        LOGGER.debug("Count of DH Clusters with Autoscaling by environmentcrn '{}' is '{}'", environmentCrn, countByEnvironmentCrn);
        return countByEnvironmentCrn;
    }

    private void calculateClusterStateMetrics() {
        periscopeMetricService.gauge(MetricType.CLUSTER_STATE_ACTIVE,
                clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(RUNNING, true, periscopeNodeConfig.getId()));
        periscopeMetricService.gauge(MetricType.CLUSTER_STATE_SUSPENDED,
                clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(SUSPENDED, true, periscopeNodeConfig.getId()));
    }

    public void updateClusterDeleted(Long clusterId, ClusterState state, int deleteRetryCount) {
        clusterRepository.updateClusterDeleted(clusterId, state, deleteRetryCount);
    }
}

package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;
import static com.sequenceiq.periscope.api.model.ClusterState.SUSPENDED;
import static com.sequenceiq.periscope.service.NotFoundException.notFound;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.ScalingConfigurationRequest;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.MetricType;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.MonitoredStack;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.FailedNodeRepository;
import com.sequenceiq.periscope.repository.SecurityConfigRepository;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeConfig;

@Service
public class ClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterService.class);

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private FailedNodeRepository failedNodeRepository;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private AlertService alertService;

    @Inject
    private PeriscopeNodeConfig periscopeNodeConfig;

    @Inject
    private PeriscopeMetricService metricService;

    public Cluster create(MonitoredStack stack, ClusterState clusterState, ClusterPertain clusterPertain) {
        return create(new Cluster(), clusterPertain, stack, clusterState);
    }

    @PostConstruct
    protected void init() {
        calculateClusterStateMetrics();
    }

    public Cluster create(Cluster cluster, ClusterPertain clusterPertain, MonitoredStack stack, ClusterState clusterState) {
        cluster.setClusterPertain(clusterPertain);
        cluster.setStackName(stack.getStackName());
        cluster.setClusterManager(stack.getClusterManager());
        cluster.setStackCrn(stack.getStackCrn());
        cluster.setStackId(stack.getStackId());
        cluster.setStackType(stack.getStackType());
        cluster.setTunnel(stack.getTunnel());
        if (stack.getCloudPlatform() != null) {
            cluster.setCloudPlatform(stack.getCloudPlatform().toUpperCase());
        }
        if (clusterState != null) {
            cluster.setState(clusterState);
        }
        cluster = save(cluster);
        if (stack.getSecurityConfig() != null) {
            SecurityConfig securityConfig = stack.getSecurityConfig();
            securityConfig.setCluster(cluster);
            securityConfigRepository.save(securityConfig);
        }
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
        addPrometheusAlertsToConsul(cluster);
        calculateClusterStateMetrics();
        return cluster;
    }

    public List<Cluster> findAllByUser(CloudbreakUser user) {
        return clusterRepository.findByUserId(user.getUserId());
    }

    public List<Cluster> findDistroXByUser(CloudbreakUser user) {
        return clusterRepository.findByUserIdAndStackType(user.getUserId(), StackType.WORKLOAD);
    }

    public Cluster findOneByStackId(Long stackId) {
        return clusterRepository.findByStackId(stackId);
    }

    public Optional<Cluster> findOneByStackCrnAndUserId(String stackCrn, CloudbreakUser cloudbreakUser) {
        return  clusterRepository.findByStackCrnAndUserId(stackCrn, cloudbreakUser.getUserId());
    }

    public Optional<Cluster> findOneByStackNameAndUserId(String stackName, CloudbreakUser cloudbreakUser) {
        return  clusterRepository.findByStackNameAndUserId(stackName, cloudbreakUser.getUserId());
    }

    public Cluster save(Cluster cluster) {
        return clusterRepository.save(cluster);
    }

    public Cluster findById(Long clusterId) {
        Cluster cluster = clusterRepository.findById(clusterId).orElseThrow(notFound("Cluster", clusterId));
        MDCBuilder.buildMdcContext(cluster);
        return cluster;
    }

    public void removeById(Long clusterId) {
        Cluster cluster = findById(clusterId);
        MDCBuilder.buildMdcContext(cluster);
        failedNodeRepository.deleteByClusterId(clusterId);
        clusterRepository.delete(cluster);
        calculateClusterStateMetrics();
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
        MDCBuilder.buildMdcContext(cluster);
        addPrometheusAlertsToConsul(cluster);
        return setState(cluster, state);
    }

    public Cluster setState(Cluster cluster, ClusterState state) {
        cluster.setState(state);
        cluster = clusterRepository.save(cluster);
        calculateClusterStateMetrics();
        return cluster;
    }

    public Cluster setAutoscaleState(Long clusterId, Boolean enableAutoscaling) {
        Cluster cluster = findById(clusterId);
        MDCBuilder.buildMdcContext(cluster);
        cluster.setAutoscalingEnabled(enableAutoscaling);
        addPrometheusAlertsToConsul(cluster);
        cluster = clusterRepository.save(cluster);
        calculateClusterStateMetrics();
        return cluster;
    }

    public String findStackCrnById(Long clusterId) {
        return clusterRepository.findStackCrnById(clusterId);
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

    public List<Long> findLoadAlertClustersForNode(StackType stackType, ClusterState state,
            boolean autoscalingEnabled, String nodeId) {
        return clusterRepository.findByLoadAlertAndStackTypeAndClusterStateAndAutoscaling(stackType, state, autoscalingEnabled, nodeId);
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
            throw new BadRequestException("Cluster exists for the same Cloudbreak stack crn and " + stack.getClusterManager().getVariant().name() + " host.");
        }
    }

    private void addPrometheusAlertsToConsul(Cluster cluster) {
        if (RUNNING.equals(cluster.getState())) {
            alertService.addPrometheusAlertsToConsul(cluster);
        }
    }

    private void calculateClusterStateMetrics() {
        metricService.submit(MetricType.CLUSTER_STATE_ACTIVE,
                clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(RUNNING, true, periscopeNodeConfig.getId()));
        metricService.submit(MetricType.CLUSTER_STATE_SUSPENDED,
                clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(SUSPENDED, true, periscopeNodeConfig.getId()));
    }

    public void deleteAlertsForCluster(Long clusterId) {
        Cluster cluster = findById(clusterId);
        cluster.getLoadAlerts().clear();
        cluster.getTimeAlerts().clear();
        save(cluster);
    }
}

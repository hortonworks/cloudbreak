package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.List;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.ScalingConfigurationRequest;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.SecurityConfigRepository;
import com.sequenceiq.periscope.repository.UserRepository;

@Service
public class ClusterService {

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private AlertService alertService;

    public Cluster create(PeriscopeUser user, AmbariStack stack, ClusterState clusterState) {
        return create(new Cluster(), user, stack, clusterState);
    }

    public Cluster create(Cluster cluster, PeriscopeUser user, AmbariStack stack, ClusterState clusterState) {
        PeriscopeUser periscopeUser = createUserIfAbsent(user);
        cluster.setUser(periscopeUser);
        cluster.setAmbari(stack.getAmbari());
        cluster.setStackId(stack.getStackId());

        if (clusterState != null) {
            cluster.setState(clusterState);
        }
        cluster = save(cluster);
        if (stack.getSecurityConfig() != null) {
            SecurityConfig securityConfig = stack.getSecurityConfig();
            securityConfig.setCluster(cluster);
            securityConfigRepository.save(securityConfig);
        }
        return cluster;
    }

    public Cluster update(Long clusterId, AmbariStack stack, boolean enableAutoscaling) {
        return update(clusterId, stack, true, null, enableAutoscaling);
    }

    public Cluster update(Long clusterId, AmbariStack stack, boolean withPermissionCheck, ClusterState clusterState, boolean enableAutoscaling) {
        Cluster cluster = withPermissionCheck ? findOneById(clusterId) : find(clusterId);
        ClusterState newState = clusterState != null ? clusterState : cluster.getState();
        cluster.setState(newState);
        cluster.setAutoscalingEnabled(enableAutoscaling);
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
        return cluster;
    }

    public List<Cluster> findAllByUser(PeriscopeUser user) {
        return clusterRepository.findByUserId(user.getId());
    }

    public Cluster findOneById(Long clusterId) {
        return clusterRepository.findOne(clusterId);
    }

    public Cluster findOneByStackId(Long stackId) {
        return clusterRepository.findByStackId(stackId);
    }

    public Cluster save(Cluster cluster) {
        return clusterRepository.save(cluster);
    }

    public Cluster find(Long clusterId) {
        return clusterRepository.findById(clusterId);
    }

    public void removeOne(Long clusterId) {
        Cluster cluster = findOneById(clusterId);
        clusterRepository.delete(cluster);
    }

    public void removeById(Long clusterId) {
        Cluster cluster = find(clusterId);
        clusterRepository.delete(cluster);
    }

    public Cluster updateScalingConfiguration(Long clusterId, ScalingConfigurationRequest scalingConfiguration) {
        Cluster cluster = findOneById(clusterId);
        cluster.setMinSize(scalingConfiguration.getMinSize());
        cluster.setMaxSize(scalingConfiguration.getMaxSize());
        cluster.setCoolDown(scalingConfiguration.getCoolDown());
        return save(cluster);
    }

    public ScalingConfigurationRequest getScalingConfiguration(Long clusterId) {
        Cluster cluster = findOneById(clusterId);
        ScalingConfigurationRequest configuration = new ScalingConfigurationRequest();
        configuration.setCoolDown(cluster.getCoolDown());
        configuration.setMaxSize(cluster.getMaxSize());
        configuration.setMinSize(cluster.getMinSize());
        return configuration;
    }

    public Cluster setState(Long clusterId, ClusterState state) {
        Cluster cluster = findOneById(clusterId);
        cluster.setState(state);
        addPrometheusAlertsToConsul(cluster);
        return clusterRepository.save(cluster);
    }

    public Cluster setAutoscaleState(Long clusterId, boolean enableAutoscaling) {
        Cluster cluster = findOneById(clusterId);
        cluster.setAutoscalingEnabled(enableAutoscaling);
        addPrometheusAlertsToConsul(cluster);
        return clusterRepository.save(cluster);
    }

    public List<Cluster> findAllByStateAndNode(ClusterState state, String nodeId) {
        return clusterRepository.findByStateAndPeriscopeNodeId(state, nodeId);
    }

    public List<Cluster> findAllForNode(ClusterState state, boolean autoscalingEnabled, String nodeId) {
        return clusterRepository.findByStateAndAutoscalingEnabledAndPeriscopeNodeId(state, autoscalingEnabled, nodeId);
    }

    public void validateClusterUniqueness(AmbariStack stack) {
        Iterable<Cluster> clusters = clusterRepository.findAll();
        boolean clusterForTheSameStackAndAmbari = StreamSupport.stream(clusters.spliterator(), false)
                .anyMatch(cluster -> {
                    boolean equalityOfStackId = cluster.getStackId() != null && cluster.getStackId().equals(stack.getStackId());
                    Ambari ambari = cluster.getAmbari();
                    Ambari newAmbari = stack.getAmbari();
                    boolean ambariObjectsNotNull = ambari != null && newAmbari != null;
                    boolean ambariHostsNotEmpty = ambariObjectsNotNull && !isEmpty(ambari.getHost()) && !isEmpty(newAmbari.getHost());
                    boolean equalityOfAmbariHost = ambariObjectsNotNull && ambariHostsNotEmpty && ambari.getHost().equals(newAmbari.getHost());
                    return equalityOfStackId && equalityOfAmbariHost;
                });
        if (clusterForTheSameStackAndAmbari) {
            throw new BadRequestException("Cluster exists for the same Cloudbreak stack id and Ambari host.");
        }
    }

    private PeriscopeUser createUserIfAbsent(PeriscopeUser user) {
        PeriscopeUser periscopeUser = userRepository.findOne(user.getId());
        if (periscopeUser == null) {
            periscopeUser = userRepository.save(user);
        }
        return periscopeUser;
    }

    private void addPrometheusAlertsToConsul(Cluster cluster) {
        if (RUNNING.equals(cluster.getState())) {
            alertService.addPrometheusAlertsToConsul(cluster);
        }
    }

}

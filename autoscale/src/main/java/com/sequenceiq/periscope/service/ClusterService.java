package com.sequenceiq.periscope.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.ScalingConfigurationJson;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.SecurityConfigRepository;
import com.sequenceiq.periscope.repository.UserRepository;

@Service
public class ClusterService {

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityConfigRepository securityConfigRepository;

    @Autowired
    private AlertService alertService;

    public Cluster create(PeriscopeUser user, AmbariStack stack) {
        PeriscopeUser periscopeUser = createUserIfAbsent(user);
        Cluster cluster = new Cluster(periscopeUser, stack);
        cluster = save(cluster);
        if (stack.getSecurityConfig() != null) {
            SecurityConfig securityConfig = stack.getSecurityConfig();
            securityConfig.setCluster(cluster);
            securityConfigRepository.save(securityConfig);
        }
        alertService.addPeriscopeAlerts(cluster);
        return cluster;
    }

    public Cluster update(long clusterId, AmbariStack stack) {
        Cluster cluster = findOneByUser(clusterId);
        cluster.update(stack);
        cluster = save(cluster);
        if (stack.getSecurityConfig() != null) {
            SecurityConfig updatedConfig = stack.getSecurityConfig();
            SecurityConfig securityConfig = securityConfigRepository.findByClusterId(clusterId);
            securityConfig.update(updatedConfig);
            securityConfigRepository.save(securityConfig);
        }
        return cluster;
    }

    public List<Cluster> findAllByUser(PeriscopeUser user) {
        return clusterRepository.findAllByUser(user.getId());
    }

    public Cluster findOneByUser(long clusterId) {
        return clusterRepository.findOne(clusterId);
    }

    public Cluster save(Cluster cluster) {
        return clusterRepository.save(cluster);
    }

    public Cluster find(long clusterId) {
        return clusterRepository.find(clusterId);
    }

    public void remove(long clusterId) {
        Cluster cluster = findOneByUser(clusterId);
        clusterRepository.delete(cluster);
    }

    public void updateScalingConfiguration(long clusterId, ScalingConfigurationJson scalingConfiguration) {
        Cluster cluster = findOneByUser(clusterId);
        cluster.setMinSize(scalingConfiguration.getMinSize());
        cluster.setMaxSize(scalingConfiguration.getMaxSize());
        cluster.setCoolDown(scalingConfiguration.getCoolDown());
        save(cluster);
    }

    public ScalingConfigurationJson getScalingConfiguration(long clusterId) {
        Cluster cluster = findOneByUser(clusterId);
        ScalingConfigurationJson configuration = new ScalingConfigurationJson();
        configuration.setCoolDown(cluster.getCoolDown());
        configuration.setMaxSize(cluster.getMaxSize());
        configuration.setMinSize(cluster.getMinSize());
        return configuration;
    }

    public Cluster setState(long clusterId, ClusterState state) {
        Cluster cluster = findOneByUser(clusterId);
        cluster.setState(state);
        return clusterRepository.save(cluster);
    }

    public List<Cluster> findAll(ClusterState state) {
        return clusterRepository.findAllByState(state);
    }

    private PeriscopeUser createUserIfAbsent(PeriscopeUser user) {
        PeriscopeUser periscopeUser = userRepository.findOne(user.getId());
        if (periscopeUser == null) {
            periscopeUser = userRepository.save(user);
        }
        return periscopeUser;
    }

}

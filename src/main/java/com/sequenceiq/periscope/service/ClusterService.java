package com.sequenceiq.periscope.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.model.HostResolution;
import com.sequenceiq.periscope.registry.ClusterRegistry;
import com.sequenceiq.periscope.registry.ClusterState;
import com.sequenceiq.periscope.registry.ConnectionException;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.UserRepository;
import com.sequenceiq.periscope.rest.json.ScalingConfigurationJson;

@Service
public class ClusterService {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(ClusterService.class);
    private static final String ROOT_PREFIX = "yarn.scheduler.capacity.root.";

    @Autowired
    private ClusterRegistry clusterRegistry;
    @Autowired
    private ClusterRepository clusterRepository;
    @Autowired
    private UserRepository userRepository;

    @Value("${periscope.hostname.resolution:private}")
    private String hostNameResolution;

    public Cluster add(PeriscopeUser user, AmbariStack stack) throws ConnectionException {
        PeriscopeUser periscopeUser = userRepository.findOne(user.getId());
        if (periscopeUser == null) {
            periscopeUser = userRepository.save(user);
        }
        Cluster cluster = new Cluster(periscopeUser, stack, HostResolution.valueOf(hostNameResolution.toUpperCase()));
        cluster.start();
        clusterRepository.save(cluster);
        return clusterRegistry.add(user, cluster);
    }

    public Cluster modify(PeriscopeUser user, long clusterId, AmbariStack stack) throws ClusterNotFoundException, ConnectionException {
        Cluster cluster = get(user, clusterId);
        try {
            Cluster probe = new Cluster(user, stack, HostResolution.valueOf(hostNameResolution.toUpperCase()));
            probe.start();
            probe.stop();
        } catch (ConnectionException e) {
            LOGGER.warn(clusterId, "Cannot modify the cluster as it fails to connect to Ambari", e);
            throw e;
        }
        cluster.setAmbari(stack.getAmbari());
        cluster.setStackId(stack.getStackId());
        cluster.refreshConfiguration();
        clusterRepository.save(cluster);
        return cluster;
    }

    public Cluster get(PeriscopeUser user, long clusterId) throws ClusterNotFoundException {
        Cluster cluster = clusterRegistry.get(user, clusterId);
        if (cluster == null) {
            throw new ClusterNotFoundException(clusterId);
        }
        return cluster;
    }

    public Cluster get(long clusterId) throws ClusterNotFoundException {
        Cluster cluster = clusterRegistry.get(clusterId);
        if (cluster == null) {
            throw new ClusterNotFoundException(clusterId);
        }
        return cluster;
    }

    public List<Cluster> getAll(PeriscopeUser user) {
        return clusterRegistry.getAll(user);
    }

    public void remove(PeriscopeUser user, long clusterId) throws ClusterNotFoundException {
        Cluster cluster = clusterRegistry.remove(user, clusterId);
        if (cluster == null) {
            throw new ClusterNotFoundException(clusterId);
        }
        clusterRepository.delete(cluster);
    }

    public void setScalingConfiguration(PeriscopeUser user, long clusterId, ScalingConfigurationJson scalingConfiguration)
            throws ClusterNotFoundException {
        Cluster cluster = get(user, clusterId);
        cluster.setMinSize(scalingConfiguration.getMinSize());
        cluster.setMaxSize(scalingConfiguration.getMaxSize());
        cluster.setCoolDown(scalingConfiguration.getCoolDown());
        clusterRepository.save(cluster);
    }

    public ScalingConfigurationJson getScalingConfiguration(PeriscopeUser user, long clusterId) throws ClusterNotFoundException {
        Cluster cluster = get(user, clusterId);
        ScalingConfigurationJson configuration = new ScalingConfigurationJson();
        configuration.setCoolDown(cluster.getCoolDown());
        configuration.setMaxSize(cluster.getMaxSize());
        configuration.setMinSize(cluster.getMinSize());
        return configuration;
    }

    public Cluster setState(PeriscopeUser user, long clusterId, ClusterState state)
            throws ClusterNotFoundException, ConnectionException {
        Cluster cluster = get(user, clusterId);
        cluster.setState(state);
        if (ClusterState.RUNNING == state) {
            try {
                cluster.start();
            } catch (ConnectionException e) {
                cluster.setState(ClusterState.SUSPENDED);
                cluster.stop();
                throw e;
            }
        } else {
            cluster.stop();
        }
        clusterRepository.save(cluster);
        return cluster;
    }

    public Cluster refreshConfiguration(PeriscopeUser user, long clusterId) throws ConnectionException, ClusterNotFoundException {
        Cluster cluster = get(user, clusterId);
        cluster.refreshConfiguration();
        return cluster;
    }

}

package com.sequenceiq.cloudbreak.service;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommand;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.repository.ClusterCommandRepository;

@Service
public class ClusterCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCommandService.class);

    @Inject
    private ClusterCommandRepository clusterCommandRepository;

    public void deleteByClusterId(Long clusterId) {
        LOGGER.debug("Deleting clustercommand by clusterid {}", clusterId);
        clusterCommandRepository.deleteByClusterId(clusterId);
    }

    public void delete(ClusterCommand clusterCommand) {
        clusterCommandRepository.delete(clusterCommand);
    }

    public Optional<ClusterCommand> findTopByClusterIdAndClusterCommandType(long clusterId, ClusterCommandType clusterCommandType) {
        return clusterCommandRepository.findTopByClusterIdAndClusterCommandType(clusterId, clusterCommandType);
    }

    public ClusterCommand save(ClusterCommand clusterCommand) {
        LOGGER.debug("Saving clustercommand {}", clusterCommand);
        return clusterCommandRepository.save(clusterCommand);
    }
}

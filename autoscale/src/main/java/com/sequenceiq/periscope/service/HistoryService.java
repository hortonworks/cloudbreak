package com.sequenceiq.periscope.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.repository.HistoryRepository;

@Service
public class HistoryService {

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    public History createEntry(ScalingStatus scalingStatus, String statusReason, int originalNodeCount, ScalingPolicy scalingPolicy) {
        History history = new History(scalingStatus, statusReason, originalNodeCount)
                .withScalingPolicy(scalingPolicy)
                .withAlert(scalingPolicy.getAlert())
                .withCluster(scalingPolicy.getAlert().getCluster());
        PeriscopeUser periscopeUser = authenticatedUserService.getPeriscopeUser();
        if (periscopeUser != null) {
            history.setUser(periscopeUser.getId());
        }
        return historyRepository.save(history);
    }

    public History createEntry(ScalingStatus scalingStatus, String statusReason, int originalNodeCount, Cluster cluster) {
        History history = new History(scalingStatus, statusReason, originalNodeCount)
                .withCluster(cluster);
        PeriscopeUser periscopeUser = authenticatedUserService.getPeriscopeUser();
        if (periscopeUser != null) {
            history.setUser(periscopeUser.getId());
        }
        return historyRepository.save(history);
    }

    public List<History> getHistory(long clusterId) {
        clusterService.findById(clusterId);
        return historyRepository.findAllByCluster(clusterId);
    }

    public History getHistory(long clusterId, long historyId) {
        return historyRepository.findByCluster(clusterId, historyId);
    }
}

package com.sequenceiq.periscope.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.repository.HistoryRepository;

@Service
public class HistoryService {

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private ClusterService clusterService;

    public History createEntry(ScalingStatus scalingStatus, String statusReason, int originalNodeCount, ScalingPolicy scalingPolicy) {
        History history = new History(scalingStatus, statusReason, originalNodeCount)
                .withScalingPolicy(scalingPolicy)
                .withAlert(scalingPolicy.getAlert())
                .withCluster(scalingPolicy.getAlert().getCluster());
        return historyRepository.save(history);
    }

    public List<History> getHistory(long clusterId) {
        clusterService.findOneById(clusterId);
        return historyRepository.findAllByCluster(clusterId);
    }

    public History getHistory(long clusterId, long historyId) {
        return historyRepository.findByCluster(clusterId, historyId);
    }
}

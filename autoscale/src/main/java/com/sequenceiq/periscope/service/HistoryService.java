package com.sequenceiq.periscope.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.repository.HistoryRepository;

@Service
public class HistoryService {

    @Autowired
    private HistoryRepository historyRepository;

    public History createEntry(ScalingStatus scalingStatus, String statusReason, int originalNodeCount, int adjustment, ScalingPolicy scalingPolicy) {
        History history = new History(scalingStatus, statusReason, originalNodeCount, adjustment)
                .withScalingPolicy(scalingPolicy)
                .withAlert(scalingPolicy.getAlert())
                .withCluster(scalingPolicy.getCluster());
        return historyRepository.save(history);
    }

    public History createEntry(ScalingStatus scalingStatus, String statusReason, Cluster cluster) {
        History history = new History(scalingStatus, statusReason, cluster.getStackCrn()).withCluster(cluster);
        return historyRepository.save(history);
    }

    public List<History> getHistory(Long clusterId, Integer historyCount) {
        PageRequest pageable = PageRequest.of(0, historyCount, Sort.by("id").descending());
        return historyRepository.findByClusterId(clusterId, pageable);
    }
}

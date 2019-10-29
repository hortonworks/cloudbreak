package com.sequenceiq.periscope.service.evaluator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.ClusterManagerVariant;
import com.sequenceiq.periscope.monitor.evaluator.ClusterCreationEvaluator;

@Service
public class ClusterCreationEvaluatorService {

    @Inject
    private List<ClusterCreationEvaluator> clusterCreationEvaluators;

    private Map<ClusterManagerVariant, Class<? extends ClusterCreationEvaluator>> map;

    @PostConstruct
    public void init() {
        map = buildMap();
    }

    public Class<? extends ClusterCreationEvaluator> get(ClusterManagerVariant variant) {
        return map.get(variant);
    }

    private Map<ClusterManagerVariant, Class<? extends ClusterCreationEvaluator>> buildMap() {
        return clusterCreationEvaluators.stream()
                .collect(Collectors.toMap(
                        ClusterCreationEvaluator::getSupportedClusterManagerVariant,
                        ClusterCreationEvaluator::getClass
                ));
    }
}

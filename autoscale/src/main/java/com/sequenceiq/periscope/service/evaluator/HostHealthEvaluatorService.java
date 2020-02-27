package com.sequenceiq.periscope.service.evaluator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.periscope.monitor.evaluator.ClusterManagerSpecificHostHealthEvaluator;

@Service
public class HostHealthEvaluatorService {

    @Inject
    private List<ClusterManagerSpecificHostHealthEvaluator> hostHealthEvaluators;

    private Map<ClusterManagerVariant, ClusterManagerSpecificHostHealthEvaluator> map;

    @PostConstruct
    public void init() {
        map = buildMap();
    }

    public ClusterManagerSpecificHostHealthEvaluator get(ClusterManagerVariant variant) {
        return map.get(variant);
    }

    private Map<ClusterManagerVariant, ClusterManagerSpecificHostHealthEvaluator> buildMap() {
        return hostHealthEvaluators.stream()
                .collect(Collectors.toMap(
                        ClusterManagerSpecificHostHealthEvaluator::getSupportedClusterManagerVariant,
                        hostHealthEvaluator -> hostHealthEvaluator
                ));
    }

}

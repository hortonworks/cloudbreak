package com.sequenceiq.periscope.service.evaluator;

import com.sequenceiq.periscope.domain.ClusterManagerVariant;
import com.sequenceiq.periscope.monitor.evaluator.ClusterManagerTotalHostsEvaluator;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TotalHostsEvaluatorService {

    @Inject
    private List<ClusterManagerTotalHostsEvaluator> totalHostsEvaluators;

    private Map<ClusterManagerVariant, ClusterManagerTotalHostsEvaluator> map;

    @PostConstruct
    public void init() {
        map = buildMap();
    }

    public ClusterManagerTotalHostsEvaluator get(ClusterManagerVariant variant) {
        return map.get(variant);
    }

    private Map<ClusterManagerVariant, ClusterManagerTotalHostsEvaluator> buildMap() {
        return totalHostsEvaluators.stream()
                .collect(Collectors.toMap(
                        ClusterManagerTotalHostsEvaluator::getSupportedClusterManagerVariant,
                        hostHealthEvaluator -> hostHealthEvaluator
                ));
    }

}

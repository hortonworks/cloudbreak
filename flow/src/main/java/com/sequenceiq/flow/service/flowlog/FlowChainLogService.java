package com.sequenceiq.flow.service.flowlog;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.cedarsoftware.util.io.JsonReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.repository.FlowChainLogRepository;

@Service
public class FlowChainLogService {

    @Inject
    private FlowChainLogRepository repository;

    public Optional<FlowChainLog> findFirstByFlowChainIdOrderByCreatedDesc(String flowChainId) {
        return repository.findFirstByFlowChainIdOrderByCreatedDesc(flowChainId);
    }

    public List<FlowChainLog> collectRelatedFlowChains(List<FlowChainLog> result, FlowChainLog flowChain) {
        result.add(flowChain);
        List<FlowChainLog> childFlowChainLogs = repository.findByParentFlowChainIdOrderByCreatedDesc(flowChain.getFlowChainId());
        childFlowChainLogs.stream().forEach(flowChainLog -> {
            if (!result.contains(flowChainLog.getFlowChainId())) {
                collectRelatedFlowChains(result, flowChainLog);
            }
        });
        return result;
    }

    public Boolean checkIfAnyFlowChainHasEventInQueue(List<FlowChainLog> flowChains) {
        Map<String, List<FlowChainLog>> similarFlowChainsMap = Maps.newHashMap();
        flowChains.forEach(flowChainLog -> {
            if (similarFlowChainsMap.containsKey(flowChainLog.getFlowChainId())) {
                similarFlowChainsMap.get(flowChainLog.getFlowChainId()).add(flowChainLog);
            } else {
                similarFlowChainsMap.put(flowChainLog.getFlowChainId(), Lists.newArrayList(flowChainLog));
            }
        });
        return similarFlowChainsMap.entrySet().stream().anyMatch(similarFlowChainsEntry -> {
            FlowChainLog latestFlowChain = similarFlowChainsEntry.getValue()
                    .stream()
                    .sorted(Comparator.comparing(FlowChainLog::getCreated).reversed())
                    .findFirst()
                    .get();
            Queue<Selectable> chain = (Queue<Selectable>) JsonReader.jsonToJava(latestFlowChain.getChain());
            return !chain.isEmpty();
        });
    }

    public int purgeOrphanFLowChainLogs() {
        return repository.purgeOrphanFLowChainLogs();
    }

    public FlowChainLog save(FlowChainLog chainLog) {
        return repository.save(chainLog);
    }

}

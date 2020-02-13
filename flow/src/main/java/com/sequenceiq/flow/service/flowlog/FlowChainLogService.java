package com.sequenceiq.flow.service.flowlog;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cedarsoftware.util.io.JsonReader;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.repository.FlowChainLogRepository;

@Service
public class FlowChainLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowChainLogService.class);

    @Inject
    private FlowChainLogRepository repository;

    public Optional<FlowChainLog> findFirstByFlowChainIdOrderByCreatedDesc(String flowChainId) {
        return repository.findFirstByFlowChainIdOrderByCreatedDesc(flowChainId);
    }

    public List<FlowChainLog> findByFlowChainIdOrderByCreatedDesc(String flowChainId) {
        return repository.findByFlowChainIdOrderByCreatedDesc(flowChainId);
    }

    public Set<FlowChainLog> collectRelatedFlowChains(FlowChainLog flowChain) {
        LOGGER.info("Finding out master flow chain based on chain id {}", flowChain.getFlowChainId());
        FlowChainLog masterFlowChain = collectMasterFlowChain(flowChain);
        LOGGER.info("Collecting child flow chains based on master chain id {}", masterFlowChain.getFlowChainId());
        Set<FlowChainLog> flowChainList = Sets.newHashSet(masterFlowChain);
        collectChildFlowChains(flowChainList, masterFlowChain);
        LOGGER.info("Collected flow chain ids for checking: {}", Joiner.on(",")
            .join(flowChainList.stream().map(flowChainLog -> flowChainLog.getFlowChainId()).collect(Collectors.toList())));
        return flowChainList;
    }

    private FlowChainLog collectMasterFlowChain(FlowChainLog flowChain) {
        Optional<FlowChainLog> lastParentFlowChain = repository.findFirstByFlowChainIdOrderByCreatedDesc(flowChain.getParentFlowChainId());
        if (lastParentFlowChain.isPresent()) {
            if (StringUtils.isNotBlank(lastParentFlowChain.get().getParentFlowChainId())) {
                return collectMasterFlowChain(lastParentFlowChain.get());
            } else {
                return lastParentFlowChain.get();
            }
        }
        return flowChain;
    }

    private void collectChildFlowChains(Set<FlowChainLog> result, FlowChainLog flowChain) {
        List<FlowChainLog> childFlowChainLogs = repository.findByParentFlowChainIdOrderByCreatedDesc(flowChain.getFlowChainId());
        childFlowChainLogs.stream().forEach(flowChainLog -> {
            result.add(flowChainLog);
            collectChildFlowChains(result, flowChainLog);
        });
    }

    public Boolean checkIfAnyFlowChainHasEventInQueue(Set<FlowChainLog> flowChains) {
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
            LOGGER.debug("Checking if chain with id {} has any event in it's queue", latestFlowChain.getFlowChainId());
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

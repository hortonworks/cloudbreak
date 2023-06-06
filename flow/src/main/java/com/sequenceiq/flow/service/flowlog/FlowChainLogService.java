package com.sequenceiq.flow.service.flowlog;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.repository.FlowChainLogRepository;

@Service
public class FlowChainLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowChainLogService.class);

    @Inject
    private FlowChainLogRepository repository;

    public Optional<FlowChainLog> findFirstByFlowChainIdOrderByCreatedDesc(String flowChainId) {
        if (flowChainId == null) {
            return Optional.empty();
        }
        return repository.findFirstByFlowChainIdOrderByCreatedDesc(flowChainId);
    }

    public String getFlowChainType(String flowChainId) {
        Optional<FlowChainLog> flowChainLog = findFirstByFlowChainIdOrderByCreatedDesc(flowChainId);
        return flowChainLog.map(FlowChainLog::getFlowChainType).orElse(null);
    }

    public List<FlowChainLog> findByFlowChainIdOrderByCreatedDesc(String flowChainId) {
        return repository.findByFlowChainIdOrderByCreatedDesc(flowChainId);
    }

    public List<FlowChainLog> getRelatedFlowChainLogs(List<FlowChainLog> sourceFlowChains) {
        Optional<FlowChainLog> flowChainWithParent = sourceFlowChains.stream()
                .filter(flowChainLog -> StringUtils.isNotBlank(flowChainLog.getParentFlowChainId())).findFirst();
        FlowChainLog lastFlowChain = sourceFlowChains.stream().max(Comparator.comparing(FlowChainLog::getCreated)
                .thenComparing(FlowChainLog::getId)).get();
        FlowChainLog inputFlowChain = flowChainWithParent.orElse(lastFlowChain);
        return collectRelatedFlowChains(inputFlowChain);
    }

    public List<FlowChainLog> collectRelatedFlowChains(FlowChainLog flowChain) {
        LOGGER.info("Finding out master flow chain based on chain id {}", flowChain.getFlowChainId());
        FlowChainLog rootFlowChain = collectRootFlowChain(flowChain);
        LOGGER.info("Collecting child flow chains based on master chain id {}", rootFlowChain.getFlowChainId());
        Map<String, FlowChainLog> flowChains = new HashMap<>();
        flowChains.put(rootFlowChain.getFlowChainId(), rootFlowChain);
        collectChildFlowChains(flowChains, rootFlowChain);
        LOGGER.info("Collected flow chain ids for checking: {}", Joiner.on(",")
                .join(flowChains.keySet()));
        return flowChains.values()
                .stream()
                .sorted(comparing(FlowChainLog::getCreated))
                .collect(toList());
    }

    public Optional<FlowChainLog> findRootInitFlowChainLog(String flowChainId) {
        Optional<FlowChainLog> initFlowChainLog = repository.findFirstByFlowChainIdOrderByCreatedAsc(flowChainId);
        while (initFlowChainLog.isPresent() && !Objects.isNull(initFlowChainLog.get().getParentFlowChainId())) {
            initFlowChainLog = repository.findFirstByFlowChainIdOrderByCreatedAsc(initFlowChainLog.get().getParentFlowChainId());
        }
        return initFlowChainLog;
    }

    private FlowChainLog collectRootFlowChain(FlowChainLog flowChain) {
        Optional<FlowChainLog> lastParentFlowChain = repository.findFirstByFlowChainIdOrderByCreatedDesc(flowChain.getParentFlowChainId());
        if (lastParentFlowChain.isPresent()) {
            if (StringUtils.isNotBlank(lastParentFlowChain.get().getParentFlowChainId())) {
                return collectRootFlowChain(lastParentFlowChain.get());
            } else {
                return lastParentFlowChain.get();
            }
        }
        return flowChain;
    }

    private void collectChildFlowChains(Map<String, FlowChainLog> flowChains, FlowChainLog flowChain) {
        List<FlowChainLog> childFlowChainLogs = repository.findByParentFlowChainIdOrderByCreatedDesc(flowChain.getFlowChainId());
        childFlowChainLogs.stream().forEach(childFlowChain -> {
            if (!flowChains.containsKey(childFlowChain.getFlowChainId())) {
                flowChains.put(childFlowChain.getFlowChainId(), childFlowChain);
                collectChildFlowChains(flowChains, childFlowChain);
            } else if (flowChains.get(childFlowChain.getFlowChainId()).getChainAsQueue().size() > childFlowChain.getChainAsQueue().size()) {
                flowChains.put(childFlowChain.getFlowChainId(), childFlowChain);
            }
        });
    }

    public boolean hasEventInFlowChainQueue(List<FlowChainLog> flowChains) {
        Map<String, List<FlowChainLog>> byFlowChainId = flowChains.stream()
                .collect(Collectors.groupingBy(FlowChainLog::getFlowChainId, toList()));
        return byFlowChainId.entrySet().stream().anyMatch(entry -> {
            FlowChainLog latestFlowChain = entry.getValue()
                    .stream()
                    .sorted(comparing(FlowChainLog::getCreated).reversed())
                    .findFirst()
                    .get();
            LOGGER.debug("Checking if chain with id {} has any event in its queue", latestFlowChain.getFlowChainId());
            LOGGER.debug("Chain string in db: {}", latestFlowChain.getChainJackson());
            Queue<Selectable> chain = latestFlowChain.getChainAsQueue();
            return !chain.isEmpty();
        });
    }

    public int purgeOrphanFlowChainLogs() {
        return repository.purgeOrphanFlowChainLogs();
    }

    public FlowChainLog save(FlowChainLog chainLog) {
        return repository.save(chainLog);
    }

    public boolean isFlowTriggeredByFlowChain(String flowChainType, Optional<FlowLogWithoutPayload> lastFlowLog) {
        if (lastFlowLog.isPresent()) {
            Optional<FlowChainLog> flowChainLog = findFirstByFlowChainIdOrderByCreatedDesc(lastFlowLog.get().getFlowChainId());
            return flowChainLog.isPresent() && flowChainLog.get().getFlowChainType().equals(flowChainType);
        }
        return false;
    }

    public Page<FlowChainLog> findAllByFlowChainIdInOrderByCreatedDesc(Set<String> flowChainIds, Pageable pageable) {
        return repository.nativeFindByFlowChainIdInOrderByCreatedDesc(flowChainIds, pageable);
    }
}

package com.sequenceiq.flow.service.flowlog;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.repository.FlowChainLogRepository;

@Service
public class FlowChainLogService {

    @Inject
    private FlowChainLogRepository repository;

    public Optional<FlowChainLog> findFirstByFlowChainIdOrderByCreatedDesc(String flowChainId) {
        return repository.findFirstByFlowChainIdOrderByCreatedDesc(flowChainId);
    }

    public int purgeOrphanFLowChainLogs() {
        return repository.purgeOrphanFLowChainLogs();
    }

    public FlowChainLog save(FlowChainLog chainLog) {
        return repository.save(chainLog);
    }

}

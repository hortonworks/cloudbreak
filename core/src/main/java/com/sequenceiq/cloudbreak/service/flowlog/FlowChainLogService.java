package com.sequenceiq.cloudbreak.service.flowlog;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.FlowChainLog;
import com.sequenceiq.cloudbreak.repository.FlowChainLogRepository;

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

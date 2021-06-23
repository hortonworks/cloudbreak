package com.sequenceiq.environment.network.service;

import java.util.Set;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.environment.environment.domain.LbUpdateFlowLog;
import com.sequenceiq.environment.environment.repository.LbUpdateFlowLogRepository;

@Service
public class LbUpdateFlowLogService {

    @Inject
    private LbUpdateFlowLogRepository repository;

    public LbUpdateFlowLog save(LbUpdateFlowLog lbUpdateFlowLog) {
        return repository.save(lbUpdateFlowLog);
    }

    public Iterable<LbUpdateFlowLog> saveAll(Iterable<LbUpdateFlowLog> lbUpdateFlowLogs) {
        return repository.saveAll(lbUpdateFlowLogs);
    }

    public Set<LbUpdateFlowLog> findByParentFlowId(String parentFlowId) {
        return repository.findByParentFlowId(parentFlowId);
    }
}

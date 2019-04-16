package com.sequenceiq.cloudbreak.service.orchestrator;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository;

@Service
public class OrchestratorService {

    @Inject
    private OrchestratorRepository repository;

    public Orchestrator save(Orchestrator orchestrator) {
        return repository.save(orchestrator);
    }

}

package com.sequenceiq.freeipa.service.stack;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.registerchildenvironment.RegisterChildEnvironmentRequest;
import com.sequenceiq.freeipa.repository.ChildEnvironmentRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class ChildEnvironmentService {

    @Inject
    private ChildEnvironmentRepository repository;

    @Inject
    private StackService stackService;

    public boolean isChildEnvironment(String environmentCrn, String accountId) {
        return repository.findParentByChildEnvironmentCrn(environmentCrn, accountId).isPresent();
    }

    public void registerChildEnvironment(RegisterChildEnvironmentRequest request, String accountId) {
        stackService.registerChildEnvironment(request, accountId);
    }
}

package com.sequenceiq.freeipa.service.stack;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.deregisterchildenv.DeregisterChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.registerchildenv.RegisterChildEnvironmentRequest;
import com.sequenceiq.freeipa.repository.ChildEnvironmentRepository;

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

    public void deregisterChildEnvironment(DeregisterChildEnvironmentRequest request, String accountId) {
        stackService.deregisterChildEnvironment(request, accountId);
    }
}

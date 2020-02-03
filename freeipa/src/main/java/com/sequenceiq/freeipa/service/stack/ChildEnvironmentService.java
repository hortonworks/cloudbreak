package com.sequenceiq.freeipa.service.stack;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.deregchildenv.DeregisterChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.regchildenv.RegisterChildEnvironmentRequest;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.ChildEnvironmentRepository;

@Service
public class ChildEnvironmentService {

    @Inject
    private ChildEnvironmentRepository repository;

    @Inject
    private StackService stackService;

    public boolean isChildEnvironment(String environmentCrn, String accountId) {
        return repository.findParentStackByChildEnvironmentCrn(environmentCrn, accountId).isPresent();
    }

    public Optional<Stack> findParentByEnvironmentCrnAndAccountId(String environmentCrn, String accountId) {
        return repository.findParentStackByChildEnvironmentCrn(environmentCrn, accountId);
    }

    public void registerChildEnvironment(RegisterChildEnvironmentRequest request, String accountId) {
        stackService.registerChildEnvironment(request, accountId);
    }

    public void deregisterChildEnvironment(DeregisterChildEnvironmentRequest request, String accountId) {
        stackService.deregisterChildEnvironment(request, accountId);
    }
}

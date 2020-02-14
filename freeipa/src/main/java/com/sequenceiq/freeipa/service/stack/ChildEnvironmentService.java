package com.sequenceiq.freeipa.service.stack;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
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

    public void attachChildEnvironment(AttachChildEnvironmentRequest request, String accountId) {
        stackService.attachChildEnvironment(request, accountId);
    }

    public void detachChildEnvironment(DetachChildEnvironmentRequest request, String accountId) {
        stackService.detachChildEnvironment(request, accountId);
    }
}

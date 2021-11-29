package com.sequenceiq.freeipa.service.stack;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.freeipa.entity.ChildEnvironment;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.ChildEnvironmentRepository;

@Service
public class ChildEnvironmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChildEnvironmentService.class);

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

    public List<Stack> findMultipleParentStackByChildEnvironmentCrnWithListsEvenIfTerminated(String environmentCrn, String accountId) {
        return repository.findMultipleParentByEnvirnmentCrnWthListsEvenIfTerminated(environmentCrn, accountId);
    }

    public Optional<Stack> findParentStackByChildEnvironmentCrnAndCrnWithListsEvenIfTerminated(String environmentCrn, String accountId, String crn) {
        return repository.findParentByEnvirnmentCrnAndCrnWthListsEvenIfTerminated(environmentCrn, accountId, crn);
    }

    public List<ChildEnvironment> findChildEnvironments(Stack stack, String accountId) {
        return repository.findByStackId(stack.getId(), accountId);
    }

    public void attachChildEnvironment(AttachChildEnvironmentRequest request, String accountId) {
        Stack stack = stackService.getByOwnEnvironmentCrnAndAccountIdWithLists(request.getParentEnvironmentCrn(), accountId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Attaching child [{}] environment to parent env [{}]", request.getChildEnvironmentCrn(), request.getParentEnvironmentCrn());
        ChildEnvironment childEnvironment = new ChildEnvironment();
        childEnvironment.setEnvironmentCrn(request.getChildEnvironmentCrn());
        childEnvironment.setStack(stack);
        repository.save(childEnvironment);
    }

    public void detachChildEnvironment(DetachChildEnvironmentRequest request, String accountId) {
        ChildEnvironment childEnvironment = repository.findByParentAndChildEnvironmentCrns(
                request.getParentEnvironmentCrn(), request.getChildEnvironmentCrn(), accountId)
            .orElseThrow(() -> new NotFoundException(String.format("ChildEnvironment by parent environment crn [%s] and child environment crn [%s] not found",
                            request.getParentEnvironmentCrn(), request.getChildEnvironmentCrn())));
        MDCBuilder.buildMdcContext(childEnvironment.getStack());
        LOGGER.info("Detaching child env: {}", childEnvironment);
        repository.delete(childEnvironment);
    }
}

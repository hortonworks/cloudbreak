package com.sequenceiq.freeipa.service.stack;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.dto.StackIdWithStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;

@Service
public class StackService {

    @Inject
    private StackRepository stackRepository;

    public List<Stack> findAllRunning() {
        return stackRepository.findAllRunning();
    }

    public Stack getByIdWithListsInTransaction(Long id) {
        return stackRepository.findOneWithLists(id).orElseThrow(() -> new NotFoundException(String.format("Stack [%s] not found", id)));
    }

    public Stack getStackById(Long id) {
        return stackRepository.findById(id).orElseThrow(() -> new NotFoundException(String.format("Stack [%s] not found", id)));
    }

    public Stack save(Stack stack) {
        return stackRepository.save(stack);
    }

    public Stack getByAccountIdEnvironmentAndName(String accountId, String environment, String name) {
        return stackRepository.findByAccountIdEnvironmentCrnAndName(accountId, environment, name)
                .orElseThrow(() -> new NotFoundException(String.format("Stack [%s] in environment [%s] not found", name, environment)));
    }

    public Stack getByEnvironmentCrnAndAccountId(String environmentCrn, String accountId) {
        return stackRepository.findByEnvironmentCrnAndAccountId(environmentCrn, accountId)
                .orElseThrow(() -> new NotFoundException(String.format("Stack by environment [%s] not found", environmentCrn)));
    }

    public List<Stack> getMultipleByEnvironmentCrnAndAccountId(Collection<String> environmentCrns, String accountId) {
        if (environmentCrns.isEmpty()) {
            return getAllByAccountId(accountId);
        } else {
            return stackRepository.findMultipleByEnvironmentCrnAndAccountId(environmentCrns, accountId);
        }
    }

    public Optional<Stack> findByEnvironmentCrnAndAccountId(String environmentCrn, String accountId) {
        return stackRepository.findByEnvironmentCrnAndAccountId(environmentCrn, accountId);
    }

    public List<Stack> findAllByEnvironmentCrnAndAccountId(String environmentCrn, String accountId) {
        return stackRepository.findAllByEnvironmentCrnAndAccountId(environmentCrn, accountId);
    }

    public List<Long> findAllIdByEnvironmentCrnAndAccountId(String environmentCrn, String accountId) {
        return stackRepository.findAllIdByEnvironmentCrnAndAccountId(environmentCrn, accountId);
    }

    public Stack getByEnvironmentCrnAndAccountIdWithLists(String environmentCrn, String accountId) {
        return stackRepository.findByEnvironmentCrnAndAccountIdWithList(environmentCrn, accountId)
                .orElseThrow(() -> new NotFoundException(String.format("Stack by environment [%s] not found", environmentCrn)));
    }

    public List<Stack> getAllByAccountId(String accountId) {
        return stackRepository.findByAccountId(accountId);
    }

    public List<StackIdWithStatus> getStatuses(Set<Long> stackIds) {
        return stackRepository.findStackStatusesWithoutAuth(stackIds);
    }

    public List<Stack> findAllWithStatuses(Collection<Status> statuses) {
        return stackRepository.findAllWithStatuses(statuses);
    }

    public List<Stack> findAllByAccountIdWithStatuses(String accountId, Collection<Status> statuses) {
        return stackRepository.findByAccountIdWithStatuses(accountId, statuses);
    }

    public List<Stack> findMultipleByEnvironmentCrnAndAccountIdWithStatuses(Collection<String> environmentCrns, String accountId, Collection<Status> statuses) {
        if (environmentCrns.isEmpty()) {
            return findAllByAccountIdWithStatuses(accountId, statuses);
        } else {
            return stackRepository.findMultipleByEnvironmentCrnAndAccountIdWithStatuses(environmentCrns, accountId, statuses);
        }
    }
}

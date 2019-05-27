package com.sequenceiq.freeipa.service.stack;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;

@Service
public class StackService {

    @Inject
    private StackRepository stackRepository;

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
        return stackRepository.findByAccountIdEnvironmentAndName(accountId, environment, name)
                .orElseThrow(() -> new NotFoundException(String.format("Stack [%s] in environment [%s] not found", name, environment)));
    }

    public Stack getByEnvironmentCrn(String environmentCrn) {
        return stackRepository.findByEnvironmentNotTerminated(environmentCrn)
                .orElseThrow(() -> new NotFoundException(String.format("Stack by environment [%s] not found", environmentCrn)));
    }

    public Optional<Stack> findByEnvironmentCrn(String environmentCrn) {
        return stackRepository.findByEnvironmentNotTerminated(environmentCrn);
    }

    public Stack getByEnvironmentCrnWithLists(String environmentCrn) {
        return stackRepository.findByEnvironmentWithListNotTerminated(environmentCrn)
                .orElseThrow(() -> new NotFoundException(String.format("Stack by environment [%s] not found", environmentCrn)));
    }

    public List<Stack> getAllByAccountId(String accountId) {
        return stackRepository.findByAccountId(accountId);
    }
}

package com.sequenceiq.freeipa.service.stackpatch;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackPatch;
import com.sequenceiq.freeipa.entity.StackPatchStatus;
import com.sequenceiq.freeipa.entity.StackPatchType;
import com.sequenceiq.freeipa.repository.StackPatchRepository;

@Service
public class StackPatchService {

    @Inject
    private StackPatchRepository stackPatchRepository;

    public StackPatch getOrCreate(Stack stack, StackPatchType stackPatchType) {
        StackPatch stackPatch = getOrCreate(stack.getId(), stackPatchType);
        stackPatch.setStack(stack);
        return stackPatch;
    }

    public StackPatch getOrCreate(Long stackId, StackPatchType stackPatchType) {
        return find(stackId, stackPatchType)
                .orElseGet(() -> create(stackId, stackPatchType));
    }

    private Optional<StackPatch> find(Long stackId, StackPatchType stackPatchType) {
        return stackPatchRepository.findByStackIdAndType(stackId, stackPatchType);
    }

    private StackPatch create(Long stackId, StackPatchType stackPatchType) {
        Stack stack = new Stack();
        stack.setId(stackId);
        return stackPatchRepository.save(new StackPatch(stack, stackPatchType));
    }

    public StackPatch updateStatus(StackPatch stackPatch, StackPatchStatus status) {
        return updateStatus(stackPatch, status, "");
    }

    public StackPatch updateStatus(StackPatch stackPatch, StackPatchStatus status, String statusReason) {
        stackPatch.setStatus(status);
        stackPatch.setStatusReason(statusReason);
        return stackPatchRepository.save(stackPatch);
    }

    public List<StackPatch> findAllByTypeForStackIds(StackPatchType stackPatchType, Collection<Long> stackIds) {
        return stackPatchRepository.findByTypeAndStackIdIn(stackPatchType, stackIds);
    }

    public List<StackPatch> findAllByTypes(Set<StackPatchType> stackPatchTypes) {
        return stackPatchRepository.findByTypeIn(stackPatchTypes);
    }

    public void deleteByStackId(Long stackId) {
        stackPatchRepository.deleteByStackId(stackId);
    }

    public void deleteAll(List<StackPatch> patches) {
        stackPatchRepository.deleteAllInBatch(patches);
    }
}

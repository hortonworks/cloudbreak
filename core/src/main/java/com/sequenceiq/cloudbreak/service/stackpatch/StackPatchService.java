package com.sequenceiq.cloudbreak.service.stackpatch;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatch;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchStatus;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.repository.StackPatchRepository;

@Service
public class StackPatchService {

    @Inject
    private StackPatchRepository stackPatchRepository;

    @Inject
    private StackPatchUsageReporterService stackPatchUsageReporterService;

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
        return updateStatus(stackPatch, status, "", false);
    }

    public StackPatch updateStatusAndReportUsage(StackPatch stackPatch, StackPatchStatus status) {
        return updateStatus(stackPatch, status, "", true);
    }

    public StackPatch updateStatusAndReportUsage(StackPatch stackPatch, StackPatchStatus status, String statusReason) {
        return updateStatus(stackPatch, status, statusReason, true);
    }

    private StackPatch updateStatus(StackPatch stackPatch, StackPatchStatus status, String statusReason, boolean reportUsage) {
        stackPatch.setStatus(status);
        stackPatch.setStatusReason(statusReason);
        if (reportUsage) {
            stackPatchUsageReporterService.reportUsage(stackPatch);
        }
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

package com.sequenceiq.stackpatcher.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.domain.StackPatchResource;
import com.sequenceiq.stackpatcher.domain.StackPatch;
import com.sequenceiq.stackpatcher.domain.StackPatchStatus;
import com.sequenceiq.stackpatcher.domain.StackPatchType;
import com.sequenceiq.stackpatcher.repository.StackPatchRepository;

@Service
public class StackPatchService {

    @Inject
    private StackPatchRepository stackPatchRepository;

    @Inject
    private StackPatchUsageReporterService stackPatchUsageReporterService;

    public StackPatch getOrCreate(StackPatchResource resource, StackPatchType stackPatchType) {
        return getOrCreate(resource.getId(), stackPatchType);
    }

    public StackPatch getOrCreate(Long resourceId, StackPatchType stackPatchType) {
        return find(resourceId, stackPatchType).orElseGet(() -> create(resourceId, stackPatchType));
    }

    public StackPatch updateStatus(StackPatch stackPatch, StackPatchStatus status) {
        return updateStatus(stackPatch, status, "");
    }

    public StackPatch updateStatus(StackPatch stackPatch, StackPatchStatus status, String statusReason) {
        stackPatch.setStatus(status);
        stackPatch.setStatusReason(statusReason);
        return stackPatchRepository.save(stackPatch);
    }

    public StackPatch updateStatusAndNotify(StackPatch stackPatch, StackPatchResource resource, StackPatchStatus status) {
        return updateStatusAndNotify(stackPatch, resource, status, "");
    }

    public StackPatch updateStatusAndNotify(StackPatch stackPatch, StackPatchResource resource, StackPatchStatus status, String statusReason) {
        StackPatch saved = updateStatus(stackPatch, status, statusReason);
        stackPatchUsageReporterService.reportStatusChange(resource, saved.getType(), status, statusReason);
        return saved;
    }

    public List<StackPatch> findAllByTypeForResourceIds(StackPatchType stackPatchType, Collection<Long> resourceIds) {
        return stackPatchRepository.findByTypeAndResourceIdIn(stackPatchType, resourceIds);
    }

    public List<StackPatch> findAllByTypes(Set<StackPatchType> stackPatchTypes) {
        return stackPatchRepository.findByTypeIn(stackPatchTypes);
    }

    public void deleteByResourceId(Long resourceId) {
        stackPatchRepository.deleteByResourceId(resourceId);
    }

    public void deleteAll(List<StackPatch> stackPatches) {
        stackPatchRepository.deleteAllInBatch(stackPatches);
    }

    public Optional<StackPatch> find(Long resourceId, StackPatchType stackPatchType) {
        return stackPatchRepository.findByResourceIdAndType(resourceId, stackPatchType);
    }

    private StackPatch create(Long resourceId, StackPatchType stackPatchType) {
        return stackPatchRepository.save(new StackPatch(resourceId, stackPatchType));
    }
}

package com.sequenceiq.freeipa.service.stackpatch;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.stackpatcher.service.StackPatchResourceService;

@Service
public class FreeIpaStackPatchResourceService implements StackPatchResourceService<Stack> {

    @Inject
    private StackService stackService;

    @Override
    public Stack loadResource(Long id) {
        return stackService.getByIdWithListsInTransaction(id);
    }

    @Override
    public boolean isUnschedulable(Stack resource) {
        return resource.getStackStatus().getStatus().isUnschedulableState();
    }

    @Override
    public List<JobResource> findAllSchedulableResources() {
        return stackService.findAllForAutoSync();
    }

    @Override
    public Optional<MdcContextInfoProvider> getMdcContextInfoProvider(Long id) {
        return Optional.ofNullable(stackService.getStackById(id));
    }

    @Override
    public JobResource getJobResource(Long id) {
        return stackService.getJobResource(id);
    }
}

package com.sequenceiq.cloudbreak.service.stackpatch;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.stackpatcher.service.StackPatchResourceService;

@Service
public class CloudbreakStackPatchResourceService implements StackPatchResourceService<Stack> {

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public Stack loadResource(Long id) {
        return stackService.getByIdWithListsInTransaction(id);
    }

    @Override
    public boolean isUnschedulable(Stack resource) {
        return Status.getUnschedulableStatuses().contains(resource.getStatus());
    }

    @Override
    public List<JobResource> findAllSchedulableResources() {
        return stackService.getAllWhereStatusNotIn(Status.getUnschedulableStatuses());
    }

    @Override
    public Optional<MdcContextInfoProvider> getMdcContextInfoProvider(Long id) {
        return Optional.ofNullable(stackDtoService.getStackViewById(id));
    }

    @Override
    public JobResource getJobResource(Long id) {
        return stackService.getJobResource(id);
    }
}

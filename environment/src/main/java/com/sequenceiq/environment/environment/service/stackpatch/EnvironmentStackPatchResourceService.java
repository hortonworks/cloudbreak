package com.sequenceiq.environment.environment.service.stackpatch;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.stackpatcher.service.StackPatchResourceService;

@Service
public class EnvironmentStackPatchResourceService implements StackPatchResourceService<Environment> {

    @Inject
    private EnvironmentService environmentService;

    @Override
    public Environment loadResource(Long id) {
        return environmentService.findEnvironmentByIdOrThrow(id);
    }

    @Override
    public boolean isUnschedulable(Environment resource) {
        return resource.isArchived() || EnvironmentStatus.skipFromStatusChecker().contains(resource.getStatus());
    }

    @Override
    public List<JobResource> findAllSchedulableResources() {
        return environmentService.findAllForAutoSync();
    }

    @Override
    public Optional<MdcContextInfoProvider> getMdcContextInfoProvider(Long id) {
        return environmentService.findEnvironmentById(id).map(MdcContextInfoProvider.class::cast);
    }

    @Override
    public JobResource getJobResource(Long id) {
        return environmentService.getJobResource(id);
    }
}

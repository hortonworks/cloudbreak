package com.sequenceiq.stackpatcher.service;

import java.util.List;
import java.util.Optional;

import com.sequenceiq.cloudbreak.common.domain.StackPatchResource;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;

public interface StackPatchResourceService<R extends StackPatchResource> {

    R loadResource(Long id);

    boolean isUnschedulable(R resource);

    List<JobResource> findAllSchedulableResources();

    Optional<MdcContextInfoProvider> getMdcContextInfoProvider(Long id);

    JobResource getJobResource(Long id);
}

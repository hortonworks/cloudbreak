package com.sequenceiq.cloudbreak.rotation.job;

import org.quartz.Job;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;

/**
 * Adapter for periodic secret rotation Quartz jobs.
 * Wraps a JobResource from listJobResources().
 */
public class PeriodicRotationJobAdapter extends JobResourceAdapter {

    public PeriodicRotationJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    public Class<? extends Job> getJobClassForResource() {
        return PeriodicSecretRotationJob.class;
    }

    @Override
    public Class<? extends JobResourceRepository> getRepositoryClassForResource() {
        return null;
    }
}

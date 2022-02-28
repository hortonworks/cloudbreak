package com.sequenceiq.cloudbreak.quartz.model;

import java.util.Optional;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.springframework.context.ApplicationContext;

public abstract class JobResourceAdapter<T> {

    public static final String LOCAL_ID = "localId";

    public static final String REMOTE_RESOURCE_CRN = "remoteResourceCrn";

    private JobResource jobResource;

    public JobResourceAdapter(Long id, ApplicationContext context) {
        loadResource(id, context);
    }

    public JobResourceAdapter(JobResource jobResource) {
        this.jobResource = jobResource;
    }

    public JobResource getJobResource() {
        return jobResource;
    }

    protected JobResourceAdapter<T> loadResource(Long id, ApplicationContext context) {
        Optional<JobResource> resource = find(id, context);
        this.jobResource = resource.orElse(null);
        return this;
    }

    private Optional<JobResource> find(Long id, ApplicationContext context) {
        JobResourceRepository<T, Long> jobResourceRepository = context.getBean(getRepositoryClassForResource());
        return jobResourceRepository.getJobResource(id);
    }

    public abstract Class<? extends Job> getJobClassForResource();

    public abstract Class<? extends JobResourceRepository<T, Long>> getRepositoryClassForResource();

    public JobDataMap toJobDataMap() {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(LOCAL_ID, getJobResource().getLocalId());
        jobDataMap.put(REMOTE_RESOURCE_CRN, getJobResource().getRemoteResourceId());
        return jobDataMap;
    }

}

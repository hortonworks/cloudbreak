package com.sequenceiq.cloudbreak.quartz.model;

import java.util.Optional;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;

public abstract class JobResourceAdapter<T> {

    public static final String LOCAL_ID = "localId";

    public static final String REMOTE_RESOURCE_CRN = "remoteResourceCrn";

    private T resource;

    public JobResourceAdapter(Long id, ApplicationContext context) {
        loadResource(id, context);
    }

    public JobResourceAdapter(T resource) {
        this.resource = resource;
    }

    public T getResource() {
        return resource;
    }

    protected JobResourceAdapter<T> loadResource(Long id, ApplicationContext context) {
        Optional<T> resource = find(id, context);
        this.resource = resource.orElse(null);
        return this;
    }

    public abstract String getLocalId();

    public abstract String getRemoteResourceId();

    private Optional<T> find(Long id, ApplicationContext context) {
        CrudRepository<T, Long> repo = context.getBean(getRepositoryClassForResource());
        return repo.findById(id);
    }

    public abstract Class<? extends Job> getJobClassForResource();

    public abstract Class<? extends CrudRepository<T, Long>> getRepositoryClassForResource();

    public JobDataMap toJobDataMap() {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(LOCAL_ID, getLocalId());
        jobDataMap.put(REMOTE_RESOURCE_CRN, getRemoteResourceId());
        return jobDataMap;
    }

}

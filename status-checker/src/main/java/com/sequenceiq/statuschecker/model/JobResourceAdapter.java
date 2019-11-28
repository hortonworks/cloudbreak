package com.sequenceiq.statuschecker.model;

import java.util.Optional;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;

public abstract class JobResourceAdapter<T> {

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
        CrudRepository repo = context.getBean(getRepositoryClassForResource());
        return (Optional<T>) repo.findById(id);
    }

    public abstract Class<? extends Job> getJobClassForResource();

    public abstract Class<? extends CrudRepository> getRepositoryClassForResource();

}

package com.sequenceiq.cloudbreak.quartz.model;

import java.util.Optional;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
public interface JobResourceRepository<T, ID> extends Repository<T, ID> {
    Optional<JobResource> getJobResource(ID resourceId);
}

package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.StackStatus;

@EntityType(entityClass = StackStatus.class)
public interface StackStatusRepository extends CrudRepository<StackStatus, Long> {
}

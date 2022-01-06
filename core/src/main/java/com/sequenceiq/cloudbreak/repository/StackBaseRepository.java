package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.domain.stack.StackBase;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@Repository
@EntityType(entityClass = StackBase.class)
public interface StackBaseRepository extends CrudRepository<StackBase, Long> {
}

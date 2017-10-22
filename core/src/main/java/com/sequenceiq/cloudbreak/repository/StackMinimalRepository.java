package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackMinimal;

@EntityType(entityClass = Stack.class)
public interface StackMinimalRepository extends CrudRepository<StackMinimal, Long> {

}

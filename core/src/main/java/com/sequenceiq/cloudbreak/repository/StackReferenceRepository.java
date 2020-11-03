package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Stack.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface StackReferenceRepository extends JpaRepository<Stack, Long> {

}

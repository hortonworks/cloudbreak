package com.sequenceiq.freeipa.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByReturnValue;
import com.sequenceiq.freeipa.entity.Stack;

@EntityType(entityClass = Stack.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface StackRepository extends DisabledBaseRepository<Stack, Long> {

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.id= :id ")
    Optional<Stack> findOneWithLists(@Param("id") Long id);

    Optional<Stack> findByNameAndEnvironment(String name, String environment);
}

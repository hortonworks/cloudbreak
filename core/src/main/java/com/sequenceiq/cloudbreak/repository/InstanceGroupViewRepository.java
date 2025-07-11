package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = InstanceGroupView.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface InstanceGroupViewRepository  extends CrudRepository<InstanceGroupView, Long> {

    @Query("SELECT i FROM InstanceGroupView i LEFT JOIN FETCH i.instanceMetaData WHERE i.stackId = :stackId")
    Set<InstanceGroupView> findInstanceGroupsInStack(@Param("stackId") Long stackId);

    @Query("SELECT i FROM InstanceGroupView i LEFT JOIN FETCH i.instanceMetaData WHERE i.stackId IN :stackIds")
    Set<InstanceGroupView> findInstanceGroupsInStacks(@Param("stackIds") Set<Long> stackIds);
}

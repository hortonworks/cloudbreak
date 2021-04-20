package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = InstanceGroupView.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface InstanceGroupViewRepository  extends CrudRepository<InstanceGroupView, Long> {

    @Query("SELECT i FROM InstanceGroupView i LEFT JOIN FETCH i.instanceMetaData WHERE i.stack.id = :stackId")
    Set<InstanceGroupView> findInstanceGroupsInStack(@Param("stackId") Long stackId);

}

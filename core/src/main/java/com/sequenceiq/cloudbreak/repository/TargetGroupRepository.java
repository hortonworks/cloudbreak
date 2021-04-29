package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = TargetGroup.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface TargetGroupRepository extends CrudRepository<TargetGroup, Long> {

    @Query("SELECT t FROM TargetGroup t INNER JOIN t.instanceGroups ig WHERE ig.id= :instanceGroupId")
    Set<TargetGroup> findByInstanceGroupId(@Param("instanceGroupId") Long instanceGroupId);

    @Query("SELECT t FROM TargetGroup t INNER JOIN t.loadBalancerSet lb WHERE lb.id= :loadBalancerId")
    Set<TargetGroup> findTargetGroupsByLoadBalancerId(@Param("loadBalancerId") Long loadBalancerId);
}

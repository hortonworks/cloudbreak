package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import java.util.Set;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

@EntityType(entityClass = TargetGroup.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface TargetGroupRepository extends CrudRepository<TargetGroup, Long> {

    @Query("SELECT t FROM TargetGroup t INNER JOIN t.instanceGroups ig WHERE ig.id= :instanceGroupId")
    Set<TargetGroup> findByInstanceGroupId(@Param("instanceGroupId") Long instanceGroupId);

    Set<TargetGroup> findByLoadBalancerId(@Param("loadBalancerId") Long loadBalancerId);
}

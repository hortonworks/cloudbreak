package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisablePermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = InstanceGroup.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisablePermission
public interface InstanceGroupRepository extends DisabledBaseRepository<InstanceGroup, Long> {

    @EntityGraph(value = "InstanceGroup.instanceMetaData", type = EntityGraphType.LOAD)
    @Query("SELECT i from InstanceGroup i WHERE i.stack.id = :stackId AND i.groupName = :groupName")
    InstanceGroup findOneByGroupNameInStack(@Param("stackId") Long stackId, @Param("groupName") String groupName);

    Set<InstanceGroup> findBySecurityGroup(SecurityGroup securityGroup);

    @EntityGraph(value = "InstanceGroup.instanceMetaData", type = EntityGraphType.LOAD)
    Set<InstanceGroup> findByStackId(@Param("stackId") Long stackId);
}
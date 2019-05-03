package com.sequenceiq.freeipa.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.SecurityGroup;

@EntityType(entityClass = InstanceGroup.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface InstanceGroupRepository extends DisabledBaseRepository<InstanceGroup, Long> {

    @EntityGraph(value = "InstanceGroup.instanceMetaData", type = EntityGraphType.LOAD)
    @Query("SELECT i from InstanceGroup i WHERE i.stack.id = :stackId AND i.groupName = :groupName")
    Optional<InstanceGroup> findOneByGroupNameInStack(@Param("stackId") Long stackId, @Param("groupName") String groupName);

    Set<InstanceGroup> findBySecurityGroup(SecurityGroup securityGroup);

    @EntityGraph(value = "InstanceGroup.instanceMetaData", type = EntityGraphType.LOAD)
    Set<InstanceGroup> findByStackId(@Param("stackId") Long stackId);

}
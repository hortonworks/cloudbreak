package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@EntityType(entityClass = InstanceGroup.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface InstanceGroupRepository extends CrudRepository<InstanceGroup, Long> {

    @EntityGraph(value = "InstanceGroup.instanceMetaData", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT i from InstanceGroup i WHERE i.stack.id = :stackId AND i.groupName = :groupName")
    InstanceGroup findOneByGroupNameInStack(@Param("stackId") Long stackId, @Param("groupName") String groupName);

    Set<InstanceGroup> findBySecurityGroup(SecurityGroup securityGroup);
}
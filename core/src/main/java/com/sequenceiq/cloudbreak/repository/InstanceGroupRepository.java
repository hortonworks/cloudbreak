package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = InstanceGroup.class)
@Transactional(TxType.REQUIRED)
public interface InstanceGroupRepository extends CrudRepository<InstanceGroup, Long> {

    @EntityGraph(value = "InstanceGroup.instanceMetaData", type = EntityGraphType.LOAD)
    @Query("SELECT i from InstanceGroup i WHERE i.stack.id = :stackId AND i.groupName = :groupName")
    Optional<InstanceGroup> findOneWithInstanceMetadataByGroupNameInStack(@Param("stackId") Long stackId, @Param("groupName") String groupName);

    @Query("SELECT i from InstanceGroup i WHERE i.stack.id = :stackId AND i.groupName = :groupName")
    Optional<InstanceGroup> findOneByStackIdAndGroupName(@Param("stackId") Long stackId, @Param("groupName") String groupName);

    @Query("SELECT i from InstanceGroup i WHERE i.stack.id = :stackId AND i.groupName IN :groupNames")
    List<InstanceGroup> findByStackIdAndInstanceGroupNames(@Param("stackId") Long stackId, @Param("groupNames") Collection<String> groupNames);

    Set<InstanceGroup> findBySecurityGroup(SecurityGroup securityGroup);

    Set<InstanceGroup> findByStackId(@Param("stackId") Long stackId);

    @Query("SELECT i.instanceGroup " +
            "FROM InstanceMetaData i " +
            "WHERE i.instanceGroup.stack.id= :stackId AND i.discoveryFQDN= :hostName AND i.instanceStatus <> 'TERMINATED'")
    Optional<InstanceGroup> findInstanceGroupInStackByHostName(@Param("stackId") Long stackId, @Param("hostName") String hostName);

    @Query("SELECT i FROM InstanceGroup i INNER JOIN i.targetGroups tg LEFT JOIN FETCH i.targetGroups WHERE tg.id= :targetGroupId")
    Set<InstanceGroup> findByTargetGroupId(@Param("targetGroupId") Long targetGroupId);

    @Query("SELECT i.instanceGroup FROM InstanceMetaData i WHERE i.instanceMetadataType = 'GATEWAY_PRIMARY' AND i.instanceStatus <> 'TERMINATED' "
            + "AND i.instanceGroup.stack.id= :stackId")
    Optional<InstanceGroup> getPrimaryGatewayInstanceGroupByStackId(@Param("stackId") Long stackId);

    @Query("SELECT i FROM InstanceGroup i JOIN FETCH i.template WHERE i.stack.id = :stackId")
    Set<InstanceGroup> getByStackAndFetchTemplates(@Param("stackId") Long stackId);
}
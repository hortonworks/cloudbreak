package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.view.AvailabilityZoneView;
import com.sequenceiq.cloudbreak.view.delegate.InstanceGroupViewDelegate;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = InstanceGroup.class)
@Transactional(TxType.REQUIRED)
public interface InstanceGroupRepository extends CrudRepository<InstanceGroup, Long> {

    String PROJECTION = "ig.id as id, " +
            "ig.groupName as groupName, " +
            "ig.instanceGroupType as instanceGroupType, " +
            "t as template, " +
            "sg as securityGroup, " +
            "ig.attributes as attributes, " +
            "ig.minimumNodeCount as minimumNodeCount, " +
            "n as instanceGroupNetwork, " +
            "ig.scalabilityOption as scalabilityOption ";

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

    @Query("SELECT " + PROJECTION +
            "FROM InstanceGroup ig " +
            "LEFT JOIN ig.targetGroups tg " +
            "LEFT JOIN ig.securityGroup sg " +
            "LEFT JOIN ig.template t " +
            "LEFT JOIN ig.instanceGroupNetwork n " +
            "WHERE tg.id= :targetGroupId")
    List<InstanceGroupViewDelegate> findByTargetGroupId(@Param("targetGroupId") Long targetGroupId);

    @Query("SELECT i.instanceGroup FROM InstanceMetaData i WHERE i.instanceMetadataType = 'GATEWAY_PRIMARY' AND i.instanceStatus <> 'TERMINATED' "
            + "AND i.instanceGroup.stack.id= :stackId")
    Optional<InstanceGroup> getPrimaryGatewayInstanceGroupByStackId(@Param("stackId") Long stackId);

    @Query("SELECT i.instanceGroup FROM InstanceMetaData i JOIN FETCH i.instanceGroup.template " +
            "WHERE i.instanceMetadataType = 'GATEWAY_PRIMARY' AND i.instanceStatus <> 'TERMINATED' AND i.instanceGroup.stack.id= :stackId")
    Optional<InstanceGroup> getPrimaryGatewayInstanceGroupWithTemplateByStackId(@Param("stackId") Long stackId);

    @EntityGraph(value = "InstanceGroup.instanceMetaData", type = EntityGraphType.LOAD)
    @Query("SELECT ig from InstanceGroup ig " +
            "LEFT JOIN FETCH ig.template t " +
            "WHERE ig.stack.id = :stackId AND ig.groupName = :groupName")
    Optional<InstanceGroup> getInstanceGroupWithTemplateAndInstancesByGroupNameInStack(@Param("stackId") Long stackId, @Param("groupName") String groupName);

    @Query("SELECT i FROM InstanceGroup i JOIN FETCH i.template WHERE i.stack.id = :stackId")
    Set<InstanceGroup> getByStackAndFetchTemplates(@Param("stackId") Long stackId);

    @Query("SELECT " + PROJECTION +
            "FROM InstanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "LEFT JOIN ig.securityGroup sg " +
            "LEFT JOIN ig.template t " +
            "LEFT JOIN ig.instanceGroupNetwork n " +
            "WHERE s.id= :stackId "
    )
    List<InstanceGroupViewDelegate> findInstanceGroupViewByStackId(@Param("stackId") Long stackId);

    @Query("SELECT " + PROJECTION +
            "FROM InstanceGroup ig " +
            "LEFT JOIN ig.securityGroup sg " +
            "LEFT JOIN ig.stack s " +
            "LEFT JOIN ig.template t " +
            "LEFT JOIN ig.instanceGroupNetwork n " +
            "WHERE s.id= :stackId " +
            "AND ig.groupName = :groupName")
    Optional<InstanceGroupViewDelegate> findInstanceGroupViewByStackIdAndGroupName(@Param("stackId") Long stackId, @Param("groupName") String groupName);

    @Query("SELECT " + PROJECTION +
            "FROM InstanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "LEFT JOIN ig.securityGroup sg " +
            "LEFT JOIN ig.template t " +
            "LEFT JOIN ig.instanceGroupNetwork n " +
            "WHERE s.id= :stackId " +
            "AND ig.groupName in :groupNames")
    List<InstanceGroupViewDelegate> findAllInstanceGroupViewByStackIdAndGroupNames(@Param("stackId") Long stackId,
            @Param("groupNames") Collection<String> groupNames);

    @Query("SELECT ig.id as instanceGroupId, " +
            "az.availabilityZone as availabilityZone " +
            "FROM AvailabilityZone az " +
            "LEFT JOIN az.instanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "WHERE s.id= :stackId "
    )
    List<AvailabilityZoneView> findAvailabilityZonesByStackId(@Param("stackId") Long stackId);

    @Query("SELECT az.availabilityZone " +
            "FROM AvailabilityZone az " +
            "LEFT JOIN az.instanceGroup ig " +
            "WHERE ig.id= :groupId"
    )
    List<String> findAvailabilityZonesByStackIdAndGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("UPDATE InstanceGroup ig SET ig.securityGroup = null, ig.template = null, ig.instanceGroupNetwork = null WHERE ig.id in :instanceGroupIds")
    void updateToNullForTermination(@Param("instanceGroupIds") List<Long> instanceGroupIds);

    @Modifying
    @Query("UPDATE InstanceGroup ig SET ig.attributes = :attributes WHERE ig.id = :instanceGroupId")
    void updateAttributes(@Param("instanceGroupId") Long instanceGroupId, @Param("attributes") Json attributes);

    @Query("SELECT i FROM InstanceGroup i JOIN FETCH i.template WHERE i.stack.id = :stackId AND i.groupName = :groupName")
    Optional<InstanceGroup> getByStackIdAndInstanceGroupNameWithFetchTemplate(@Param("stackId") Long stackId, @Param("groupName") String groupName);

}

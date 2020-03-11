package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.projection.InstanceMetaDataGroupView;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspaceId;

@EntityType(entityClass = InstanceMetaData.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface InstanceMetaDataRepository extends DisabledBaseRepository<InstanceMetaData, Long> {

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i " +
            "WHERE i.instanceGroup.stack.id= :stackId " +
            "AND i.instanceStatus <> 'TERMINATED' " +
            "AND i.instanceStatus <> 'DELETED_ON_PROVIDER_SIDE'")
    Set<InstanceMetaData> findNotTerminatedForStack(@Param("stackId") Long stackId);

    @Query("SELECT i FROM InstanceMetaData i " +
            "WHERE i.instanceGroup.stack.id= :stackId " +
            "AND i.instanceStatus <> 'TERMINATED' " +
            "AND i.instanceStatus <> 'DELETED_ON_PROVIDER_SIDE'")
    Set<InstanceMetaData> findNotTerminatedForStackWithoutInstanceGroups(@Param("stackId") Long stackId);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId")
    Set<InstanceMetaData> findAllInStack(@Param("stackId") Long stackId);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceId= :instanceId AND i.instanceGroup.stack.id= :stackId")
    Optional<InstanceMetaData> findByStackIdAndInstanceId(@Param("stackId") Long stackId, @Param("instanceId") String instanceId);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.discoveryFQDN= :hostName AND i.instanceStatus <> 'TERMINATED'")
    Optional<InstanceMetaData> findHostInStack(@Param("stackId") Long stackId, @Param("hostName") String hostName);

    @Query("SELECT i.instanceGroup.groupName as groupName, i.instanceGroup.instanceGroupType as instanceGroupType " +
            "FROM InstanceMetaData i " +
            "WHERE i.instanceGroup.stack.id= :stackId AND i.discoveryFQDN= :hostName AND i.instanceStatus <> 'TERMINATED'")
    Optional<InstanceMetaDataGroupView> findInstanceGroupViewInClusterByName(@Param("stackId") Long stackId, @Param("hostName") String hostName);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.discoveryFQDN= :hostName AND i.instanceStatus <> 'TERMINATED'")
    Optional<InstanceMetaData> findHostInStackWithoutInstanceGroup(@Param("stackId") Long stackId, @Param("hostName") String hostName);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.id= :instanceGroupId AND i.instanceStatus in ('CREATED')")
    Set<InstanceMetaData> findUnusedHostsInInstanceGroup(@Param("instanceGroupId") Long instanceGroupId);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.groupName= :instanceGroupName "
            + "AND i.instanceStatus in ('CREATED') AND i.instanceGroup.stack.id= :stackId")
    Set<InstanceMetaData> findUnusedHostsInInstanceGroup(@Param("stackId") Long stackId, @Param("instanceGroupName") String instanceGroupName);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.id = :instanceGroupId AND i.instanceStatus <> 'TERMINATED'")
    List<InstanceMetaData> findAliveInstancesInInstanceGroup(@Param("instanceGroupId") Long instanceGroupId);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.instanceGroup.groupName= :groupName "
            + "AND i.instanceStatus in ('CREATED', 'SERVICES_RUNNING', 'DECOMMISSIONED', 'FAILED', 'STOPPED')")
    Set<InstanceMetaData> findRemovableInstances(@Param("stackId") Long stackId, @Param("groupName") String groupName);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.privateIp= :privateAddress AND i.instanceStatus <> 'TERMINATED'")
    Optional<InstanceMetaData> findNotTerminatedByPrivateAddress(@Param("stackId") Long stackId, @Param("privateAddress") String privateAddress);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraph.EntityGraphType.LOAD)
    List<InstanceMetaData> findAllByInstanceGroupAndInstanceStatus(InstanceGroup instanceGroup, InstanceStatus status);

    @Query("SELECT i.serverCert FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.instanceMetadataType = 'GATEWAY_PRIMARY' "
            + "AND i.instanceStatus <> 'TERMINATED'")
    Optional<String> getServerCertByStackId(@Param("stackId") Long stackId);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceMetadataType = 'GATEWAY_PRIMARY' AND i.instanceStatus <> 'TERMINATED' "
            + "AND i.instanceGroup.stack.id= :stackId")
    Optional<InstanceMetaData> getPrimaryGatewayInstanceMetadata(@Param("stackId") Long stackId);

    @Query("SELECT i.discoveryFQDN FROM InstanceMetaData i WHERE i.instanceGroup.id = :instanceGroupId AND i.instanceMetadataType = 'GATEWAY_PRIMARY' AND"
            + " i.instanceStatus <> 'TERMINATED' AND i.instanceGroup.stack.id= :stackId")
    Optional<String> getPrimaryGatewayDiscoveryFQDNByInstanceGroup(@Param("stackId") Long stackId, @Param("instanceGroupId") Long instanceGroupId);

    @Query("SELECT max(imd.privateId) FROM InstanceMetaData imd WHERE imd.instanceGroup IN :instanceGroups")
    Long getMaxPrivateId(@Param("instanceGroups") List<InstanceGroup> instanceGroups);

    @Query("SELECT s.id as stackId, COUNT(i) as instanceCount "
            + "FROM InstanceMetaData i JOIN i.instanceGroup ig JOIN ig.stack s WHERE s.workspace.id= :id "
            + "AND i.instanceStatus <> 'TERMINATED' "
            + "AND (:environmentCrn IS null OR s.environmentCrn = :environmentCrn) "
            + "AND (s.type IS null OR s.type in :stackTypes) "
            + "GROUP BY s.id")
    Set<StackInstanceCount> countByWorkspaceId(@Param("id") Long id, @Param("environmentCrn") String environmentCrn,
            @Param("stackTypes") List<StackType> stackTypes);

    @CheckPermissionsByWorkspaceId
    @Query("SELECT s.id as stackId, COUNT(i) as instanceCount "
            + "FROM InstanceMetaData i JOIN i.instanceGroup ig JOIN ig.stack s WHERE s.workspace.id= :id AND i.instanceStatus = 'SERVICES_UNHEALTHY' "
            + "GROUP BY s.id")
    Set<StackInstanceCount> countUnhealthyByWorkspaceId(@Param("id") Long workspaceId);

}

package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = InstanceMetaData.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface InstanceMetaDataRepository extends DisabledBaseRepository<InstanceMetaData, Long> {

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.instanceStatus <> 'TERMINATED'")
    Set<InstanceMetaData> findNotTerminatedForStack(@Param("stackId") Long stackId);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId")
    Set<InstanceMetaData> findAllInStack(@Param("stackId") Long stackId);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceId= :instanceId AND i.instanceGroup.stack.id= :stackId")
    Optional<InstanceMetaData> findByStackIdAndInstanceId(@Param("stackId") Long stackId, @Param("instanceId") String instanceId);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.discoveryFQDN= :hostName AND i.instanceStatus <> 'TERMINATED'")
    Optional<InstanceMetaData> findHostInStack(@Param("stackId") Long stackId, @Param("hostName") String hostName);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.id= :instanceGroupId AND i.instanceStatus in ('CREATED', 'UNREGISTERED')")
    Set<InstanceMetaData> findUnusedHostsInInstanceGroup(@Param("instanceGroupId") Long instanceGroupId);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.groupName= :instanceGroupName "
            + "AND i.instanceStatus in ('CREATED', 'UNREGISTERED') AND i.instanceGroup.stack.id= :stackId")
    Set<InstanceMetaData> findUnusedHostsInInstanceGroup(@Param("stackId") Long stackId, @Param("instanceGroupName") String instanceGroupName);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.id = :instanceGroupId AND i.instanceStatus <> 'TERMINATED'")
    List<InstanceMetaData> findAliveInstancesInInstanceGroup(@Param("instanceGroupId") Long instanceGroupId);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.instanceGroup.groupName= :groupName "
            + "AND i.instanceStatus in ('CREATED', 'UNREGISTERED', 'DECOMMISSIONED', 'FAILED', 'STOPPED')")
    Set<InstanceMetaData> findRemovableInstances(@Param("stackId") Long stackId, @Param("groupName") String groupName);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.privateIp= :privateAddress AND i.instanceStatus <> 'TERMINATED'")
    Optional<InstanceMetaData> findNotTerminatedByPrivateAddress(@Param("stackId") Long stackId, @Param("privateAddress") String privateAddress);

    List<InstanceMetaData> findAllByInstanceGroupAndInstanceStatus(InstanceGroup instanceGroup, InstanceStatus status);

    @Query("SELECT i.serverCert FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.instanceMetadataType = 'GATEWAY_PRIMARY' "
            + "AND i.instanceStatus <> 'TERMINATED'")
    Optional<String> getServerCertByStackId(@Param("stackId") Long stackId);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceMetadataType = 'GATEWAY_PRIMARY' AND i.instanceStatus <> 'TERMINATED' "
            + "AND i.instanceGroup.stack.id= :stackId")
    Optional<InstanceMetaData> getPrimaryGatewayInstanceMetadata(@Param("stackId") Long stackId);

    @Query("SELECT i.discoveryFQDN FROM InstanceMetaData i WHERE i.instanceGroup.id = :instanceGroupId AND i.instanceMetadataType = 'GATEWAY_PRIMARY' AND"
            + " i.instanceStatus <> 'TERMINATED' AND i.instanceGroup.stack.id= :stackId")
    Optional<String> getPrimaryGatewayDiscoveryFQDNByInstanceGroup(@Param("stackId") Long stackId, @Param("instanceGroupId") Long instanceGroupId);

    @Query("SELECT max(imd.privateId) FROM InstanceMetaData imd WHERE imd.instanceGroup IN :instanceGroups")
    Long getMaxPrivateId(@Param("instanceGroups") List<InstanceGroup> instanceGroups);

    @Query("SELECT s.id as stackId, COUNT(i) as instanceCount "
            + "FROM InstanceMetaData i JOIN i.instanceGroup ig JOIN ig.stack s WHERE s.workspace.id= :id AND i.instanceStatus <> 'TERMINATED' "
            + "GROUP BY s.id")
    Set<StackInstanceCount> countByWorkspaceId(@Param("id") Long id);

    @Query("SELECT i FROM InstanceMetaData i ")
    Set<InstanceMetaData> findAllRequestedByStackId(@Param("id") Long stackId);
}

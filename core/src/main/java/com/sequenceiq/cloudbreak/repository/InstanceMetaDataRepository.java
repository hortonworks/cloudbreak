package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.projection.InstanceMetaDataGroupView;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.SubnetIdWithResourceNameAndCrn;
import com.sequenceiq.cloudbreak.view.delegate.InstanceMetadataViewDelegate;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = InstanceMetaData.class)
@Transactional(TxType.REQUIRED)
public interface InstanceMetaDataRepository extends JpaRepository<InstanceMetaData, Long> {

    String PROJECTION = "i.id as id, " +
            "ig.id as instanceGroupId, " +
            "ig.groupName as instanceGroupName, " +
            "ig.instanceGroupType as instanceGroupType, " +
            "i.instanceStatus as instanceStatus, " +
            "i.instanceName as instanceName, " +
            "i.statusReason as statusReason, " +
            "i.privateId as privateId, " +
            "i.privateIp as privateIp, " +
            "i.publicIp as publicIp, " +
            "i.sshPort as sshPort, " +
            "i.instanceId as instanceId, " +
            "i.ambariServer as ambariServer, " +
            "i.clusterManagerServer as clusterManagerServer, " +
            "i.discoveryFQDN as discoveryFQDN, " +
            "i.instanceMetadataType as instanceMetadataType, " +
            "i.localityIndicator as localityIndicator, " +
            "i.startDate as startDate, " +
            "i.terminationDate as terminationDate, " +
            "i.subnetId as subnetId, " +
            "i.availabilityZone as availabilityZone, " +
            "i.image as image, " +
            "i.rackId as rackId, " +
            "i.lifeCycle as lifeCycle, " +
            "i.variant as variant, " +
            "i.serverCert as serverCert, " +
            "i.userdataSecretResourceId as userdataSecretResourceId, " +
            "i.providerInstanceType as providerInstanceType ";

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i " +
            "WHERE i.instanceGroup.stack.id= :stackId " +
            "AND i.instanceStatus not in ('TERMINATED', 'DELETED_ON_PROVIDER_SIDE', 'DELETED_BY_PROVIDER')")
    Set<InstanceMetaData> findNotDeletedForStack(@Param("stackId") Long stackId);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i " +
            "WHERE i.instanceGroup.stack.id= :stackId " +
            "AND i.instanceStatus not in ('TERMINATED', 'DELETED_ON_PROVIDER_SIDE', 'DELETED_BY_PROVIDER', 'ZOMBIE')")
    Set<InstanceMetaData> findNotTerminatedAndNotZombieForStack(@Param("stackId") Long stackId);

    @Query("SELECT i.id FROM InstanceMetaData i " +
            "WHERE i.instanceGroup.stack.id= :stackId " +
            "AND i.instanceStatus not in ('TERMINATED', 'DELETED_ON_PROVIDER_SIDE', 'DELETED_BY_PROVIDER', 'ZOMBIE', 'SERVICES_UNHEALTHY')")
    Set<Long> findNotTerminatedAndNotUnhealthyAndNotZombieIdForStack(@Param("stackId") Long stackId);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i " +
            "WHERE i.instanceGroup.stack.id= :stackId " +
            "AND i.instanceStatus not in ('TERMINATED', 'DELETED_ON_PROVIDER_SIDE', 'DELETED_BY_PROVIDER') " +
            "ORDER BY i.privateId")
    List<InstanceMetaData> findNotTerminatedAsOrderedListForStack(@Param("stackId") Long stackId);

    @Query("SELECT " + PROJECTION +
            "FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "WHERE s.id= :stackId " +
            "AND i.instanceStatus not in ('TERMINATED', 'DELETED_ON_PROVIDER_SIDE', 'DELETED_BY_PROVIDER')")
    List<InstanceMetadataViewDelegate> findNotTerminatedInstanceMetadataViewByStackId(@Param("stackId") Long stackId);

    @Query("SELECT  " + PROJECTION +
            "FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "WHERE s.id= :stackId " +
            "AND i.instanceStatus not in :statuses ")
    List<InstanceMetadataViewDelegate> findAllViewsStatusNotInForStack(@Param("stackId") Long stackId, @Param("statuses") Collection<InstanceStatus> statuses);

    @Query("SELECT i, ig, ign, s " +
            "FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "LEFT JOIN ig.instanceGroupNetwork ign " +
            "LEFT JOIN ig.stack s " +
            "WHERE s.id= :stackId " +
            "AND i.instanceStatus not in :statuses ")
    List<InstanceMetaData> findAllStatusNotInForStackWithNetwork(@Param("stackId") Long stackId, @Param("statuses") Collection<InstanceStatus> statuses);

    @Query("SELECT i " +
            "FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "WHERE s.id= :stackId " +
            "AND i.instanceStatus not in :statuses ")
    List<InstanceMetaData> findAllStatusNotInForStack(@Param("stackId") Long stackId, @Param("statuses") Collection<InstanceStatus> statuses);

    @Query("SELECT " + PROJECTION +
            "FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "WHERE s.id= :stackId " +
            "AND i.instanceStatus in :statuses ")
    List<InstanceMetadataViewDelegate> findAllStatusInForStack(@Param("stackId") Long stackId, @Param("statuses") Collection<InstanceStatus> statuses);

    @Query("SELECT " + PROJECTION +
            "FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "WHERE ig.id= :instanceGroupId " +
            "AND i.instanceStatus in :statuses ")
    List<InstanceMetadataViewDelegate> findAllStatusInForInstanceGroup(@Param("instanceGroupId") Long instanceGroupId,
            @Param("statuses") Collection<InstanceStatus> statuses);

    @Query("SELECT i FROM InstanceMetaData i " +
            "WHERE i.instanceGroup.stack.id= :stackId " +
            "AND i.instanceStatus not in ('TERMINATED', 'DELETED_ON_PROVIDER_SIDE', 'DELETED_BY_PROVIDER', 'ZOMBIE')")
    Set<InstanceMetaData> findNotTerminatedAndNotZombieForStackWithoutInstanceGroups(@Param("stackId") Long stackId);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId")
    Set<InstanceMetaData> findAllInStack(@Param("stackId") Long stackId);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.instanceStatus <> 'TERMINATED'")
    Set<InstanceMetaData> findAllWithoutInstanceGroupInStack(@Param("stackId") Long stackId);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceId= :instanceId AND i.instanceGroup.stack.id= :stackId")
    Optional<InstanceMetaData> findByStackIdAndInstanceId(@Param("stackId") Long stackId, @Param("instanceId") String instanceId);

    @Query("SELECT " + PROJECTION +
            "FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "WHERE i.instanceId in :instanceIds AND s.id= :stackId")
    List<InstanceMetadataViewDelegate> findViewByStackIdAndInstanceId(@Param("stackId") Long stackId, @Param("instanceIds") Set<String> instanceIds);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceId IN :instanceIds AND i.instanceGroup.stack.id= :stackId")
    List<InstanceMetaData> findByStackIdAndInstanceIds(@Param("stackId") Long stackId, @Param("instanceIds") Collection<String> instanceIds);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.discoveryFQDN= :hostName AND i.instanceStatus <> 'TERMINATED'")
    Optional<InstanceMetaData> findHostInStack(@Param("stackId") Long stackId, @Param("hostName") String hostName);

    @Query("SELECT i.instanceGroup.groupName as groupName, i.instanceGroup.instanceGroupType as instanceGroupType " +
            "FROM InstanceMetaData i " +
            "WHERE i.instanceGroup.stack.id= :stackId AND i.discoveryFQDN= :hostName AND i.instanceStatus <> 'TERMINATED'")
    Optional<InstanceMetaDataGroupView> findInstanceGroupViewInClusterByName(@Param("stackId") Long stackId, @Param("hostName") String hostName);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.discoveryFQDN= :hostName AND i.instanceStatus <> 'TERMINATED'")
    Optional<InstanceMetaData> findHostInStackWithoutInstanceGroup(@Param("stackId") Long stackId, @Param("hostName") String hostName);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.id= :instanceGroupId AND i.instanceStatus in ('CREATED')")
    Set<InstanceMetaData> findUnusedHostsInInstanceGroup(@Param("instanceGroupId") Long instanceGroupId);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.groupName= :instanceGroupName "
            + "AND i.instanceStatus in ('CREATED') AND i.instanceGroup.stack.id= :stackId")
    Set<InstanceMetaData> findUnusedHostsInInstanceGroup(@Param("stackId") Long stackId, @Param("instanceGroupName") String instanceGroupName);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.id = :instanceGroupId AND i.instanceStatus <> 'TERMINATED'")
    List<InstanceMetaData> findAliveInstancesInInstanceGroup(@Param("instanceGroupId") Long instanceGroupId);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.instanceGroup.groupName= :groupName "
            + "AND i.instanceStatus in ('CREATED', 'SERVICES_RUNNING', 'DECOMMISSIONED', 'FAILED', 'STOPPED', 'ZOMBIE')")
    Set<InstanceMetaData> findRemovableInstances(@Param("stackId") Long stackId, @Param("groupName") String groupName);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraphType.LOAD)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.privateIp= :privateAddress AND i.instanceStatus <> 'TERMINATED'")
    Optional<InstanceMetaData> findNotTerminatedByPrivateAddress(@Param("stackId") Long stackId, @Param("privateAddress") String privateAddress);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraphType.LOAD)
    List<InstanceMetaData> findAllByInstanceGroupAndInstanceStatus(InstanceGroup instanceGroup, InstanceStatus status);

    @EntityGraph(value = "InstanceMetaData.instanceGroup", type = EntityGraphType.LOAD)
    List<InstanceMetaData> findAllByInstanceGroupAndInstanceStatusOrderByPrivateIdAsc(InstanceGroup instanceGroup, InstanceStatus status);

    @Query("SELECT i.serverCert FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.instanceMetadataType = 'GATEWAY_PRIMARY' "
            + "AND i.instanceStatus <> 'TERMINATED'")
    Optional<String> getServerCertByStackId(@Param("stackId") Long stackId);

    @Query("SELECT " + PROJECTION +
            "FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "WHERE i.instanceMetadataType = 'GATEWAY_PRIMARY' AND i.instanceStatus <> 'TERMINATED' " +
            "AND s.id= :stackId")
    Optional<InstanceMetadataViewDelegate> getPrimaryGatewayInstanceMetadata(@Param("stackId") Long stackId);

    @Query("SELECT i.privateIp as privateIp, i.publicIp as publicIp FROM InstanceMetaData i " +
            "WHERE i.instanceMetadataType = 'GATEWAY_PRIMARY' " +
            "AND i.instanceStatus <> 'TERMINATED' " +
            "AND i.instanceGroup.stack.id= :stackId")
    Optional<Map<String, String>> getPrimaryGatewayIp(@Param("stackId") Long stackId);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.discoveryFQDN = :discoveryFQDN AND i.instanceStatus = 'TERMINATED' AND i.instanceId IS NOT null "
            + "AND i.instanceGroup.stack.id= :stackId ORDER BY i.terminationDate DESC")
    List<InstanceMetaData> getTerminatedInstanceMetadataWithInstanceIdByFQDNOrdered(@Param("stackId") Long stackId,
            @Param("discoveryFQDN") String discoveryFQDN, Pageable pageable);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceMetadataType = 'GATEWAY_PRIMARY' AND i.instanceStatus = 'TERMINATED' "
            + "AND i.instanceGroup.stack.id= :stackId ORDER BY i.terminationDate DESC")
    List<InstanceMetaData> geTerminatedPrimaryGatewayInstanceMetadataOrdered(@Param("stackId") Long stackId, Pageable pageable);

    @Query("SELECT i.discoveryFQDN FROM InstanceMetaData i WHERE i.instanceGroup.id = :instanceGroupId AND i.instanceMetadataType = 'GATEWAY_PRIMARY' AND"
            + " i.instanceStatus <> 'TERMINATED' AND i.instanceGroup.stack.id= :stackId")
    Optional<String> getPrimaryGatewayDiscoveryFQDNByInstanceGroup(@Param("stackId") Long stackId, @Param("instanceGroupId") Long instanceGroupId);

    @Query("SELECT max(imd.privateId) FROM InstanceMetaData imd WHERE imd.instanceGroup IN :instanceGroups")
    Long getMaxPrivateId(@Param("instanceGroups") List<InstanceGroup> instanceGroups);

    @Query("SELECT s.id as stackId, COUNT(i) as instanceCount "
            + "FROM InstanceMetaData i "
            + "JOIN i.instanceGroup ig "
            + "JOIN ig.stack s "
            + "WHERE s.workspace.id= :id "
            + "AND i.instanceStatus <> 'TERMINATED' "
            + "AND s.environmentCrn = :environmentCrn "
            + "AND s.type in :stackTypes "
            + "GROUP BY s.id")
    Set<StackInstanceCount> countByWorkspaceIdWithEnvironment(
            @Param("id") Long id,
            @Param("environmentCrn") String environmentCrn,
            @Param("stackTypes") List<StackType> stackTypes);

    @Query("SELECT s.id as stackId, COUNT(i) as instanceCount "
            + "FROM InstanceMetaData i "
            + "JOIN i.instanceGroup ig "
            + "JOIN ig.stack s "
            + "WHERE s.workspace.id= :id "
            + "AND i.instanceStatus <> 'TERMINATED' "
            + "AND s.type in :stackTypes "
            + "GROUP BY s.id")
    Set<StackInstanceCount> countByWorkspaceId(
            @Param("id") Long id,
            @Param("stackTypes") List<StackType> stackTypes);

    @Query("SELECT s.id as stackId, COUNT(i) as instanceCount "
            + "FROM InstanceMetaData i JOIN i.instanceGroup ig JOIN ig.stack s "
            + "WHERE i.instanceStatus <> 'TERMINATED' AND s.id = :stackId "
            + "GROUP BY s.id")
    StackInstanceCount countByStackId(@Param("stackId") Long stackId);

    @Query("SELECT COUNT(i) as instanceCount "
            + "FROM InstanceMetaData i "
            + "WHERE i.instanceStatus <> 'TERMINATED' AND i.instanceGroup.id = :instanceGroupId")
    int countByInstanceGroupId(@Param("instanceGroupId") Long instanceGroupId);

    @Query("SELECT s.id as stackId, COUNT(i) as instanceCount "
            + "FROM InstanceMetaData i JOIN i.instanceGroup ig JOIN ig.stack s WHERE s.workspace.id= :id AND i.instanceStatus = 'SERVICES_UNHEALTHY' "
            + "GROUP BY s.id")
    Set<StackInstanceCount> countUnhealthyByWorkspaceId(@Param("id") Long workspaceId);

    @Modifying
    @Query("UPDATE InstanceMetaData SET instanceStatus = :newInstanceStatus, statusReason = :newStatusReason " +
            "WHERE id = :id AND instanceStatus <> 'TERMINATED'")
    int updateStatusIfNotTerminated(@Param("id") Long id, @Param("newInstanceStatus") InstanceStatus newInstanceStatus,
            @Param("newStatusReason") String newStatusReason);

    @Modifying
    @Query("UPDATE InstanceMetaData SET instanceStatus = :newInstanceStatus, statusReason = :newStatusReason " +
            "WHERE id in :id")
    int updateAllToStatusByIds(@Param("id") Collection<Long> id, @Param("newInstanceStatus") InstanceStatus newInstanceStatus,
            @Param("newStatusReason") String newStatusReason);

    @Modifying
    @Query("UPDATE InstanceMetaData SET instanceStatus = :newInstanceStatus, statusReason = :newStatusReason, terminationDate = :terminationDate " +
            "WHERE id in :ids")
    int updateAllInstancesToTerminated(@Param("ids") Collection<Long> ids,
            @Param("newInstanceStatus") InstanceStatus newInstanceStatus,
            @Param("terminationDate") Long terminationDate,
            @Param("newStatusReason") String newStatusReason);

    @Modifying
    @Query("UPDATE InstanceMetaData SET instanceStatus = :newInstanceStatus, statusReason = :newStatusReason " +
            "WHERE id IN (:ids) AND instanceStatus <> 'TERMINATED'")
    int updateStatusIfNotTerminated(@Param("ids") Collection<Long> ids, @Param("newInstanceStatus") InstanceStatus newInstanceStatus,
            @Param("newStatusReason") String newStatusReason);

    @Modifying
    @Query("UPDATE InstanceMetaData SET serverCert = :serverCert " +
            "WHERE instanceId = :instanceId AND discoveryFQDN = :discoveryFQDN")
    int updateServerCert(@Param("serverCert") String serverCert, @Param("instanceId") String instanceId, @Param("discoveryFQDN") String discoveryFQDN);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceStatus = 'TERMINATED' AND i.instanceGroup.stack.id= :stackId " +
            "AND i.terminationDate < :thresholdTerminationDate ORDER BY i.terminationDate ASC")
    Page<InstanceMetaData> findTerminatedInstanceMetadataByStackIdAndTerminatedBefore(@Param("stackId") Long stackId,
            @Param("thresholdTerminationDate") Long thresholdTerminationDate, Pageable pageable);

    @Modifying
    @Query("DELETE FROM InstanceMetaData WHERE id IN (:metaDataIds)")
    void deleteAllByIds(@Param("metaDataIds") List<Long> metaDataIds);

    @Query("SELECT COUNT(i) FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.instanceStatus = 'STOPPED'")
    long countStoppedForStack(@Param("stackId") Long stackId);

    @Modifying
    @Query("UPDATE InstanceMetaData SET serverCert = :serverCert WHERE id = :instanceMetadataId")
    void updateServerCert(@Param("instanceMetadataId") Long instanceMetadataId, @Param("serverCert") String serverCert);

    @Modifying
    @Query("UPDATE InstanceMetaData SET instanceName = :instanceName, image = :imageJson WHERE id = :instanceMetadataId")
    void updateInstanceNameAndImage(@Param("instanceMetadataId") Long instanceMetadataId,
            @Param("instanceName") String instanceName,
            @Param("imageJson") Json imageJson);

    @Modifying
    @Query("UPDATE InstanceMetaData SET instanceName = :instanceName WHERE id = :instanceMetadataId")
    void updateInstanceName(@Param("instanceMetadataId") Long instanceMetadataId, @Param("instanceName") String instanceName);

    @Modifying
    @Query("UPDATE InstanceMetaData SET image = :imageJson WHERE id = :instanceMetadataId")
    void updateImageByInstanceMetadataId(@Param("instanceMetadataId") Long instanceMetadataId, @Param("imageJson") Json imageJson);

    @Query("SELECT " + PROJECTION +
            "FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "WHERE ig.stack.id= :stackId " +
            "AND i.discoveryFQDN in :hostNames " +
            "AND i.instanceStatus <> 'TERMINATED'" +
            "AND ig.instanceGroupType = 'CORE'")
    List<InstanceMetadataViewDelegate> findAllWorkerWithHostnamesInStack(@Param("stackId") Long stackId, @Param("hostNames") List<String> hostNames);

    @Query("SELECT i.discoveryFQDN " +
            "FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "WHERE s.id= :stackId " +
            "AND i.privateId in :privateIds")
    List<String> findAllAvailableHostNamesByPrivateIds(@Param("stackId") Long stackId, @Param("privateIds") List<Long> privateIds);

    @Query("SELECT " + PROJECTION +
            "FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "WHERE s.id= :stackId " +
            "AND i.privateId in :privateIds")
    List<InstanceMetadataViewDelegate> findInstanceMetadataViewsByStackIdAndPrivateIds(@Param("stackId") Long stackId,
            @Param("privateIds") Collection<Long> privateIds);

    @Query("SELECT im.privateId " +
            "FROM InstanceMetaData im " +
            "LEFT JOIN im.instanceGroup ig  " +
            "LEFT JOIN ig.stack s " +
            "WHERE s.id = :stackId " +
            "ORDER BY im.privateId desc")
    Page<Long> findLastPrivateIdForStack(@Param("stackId") Long stackId, Pageable page);

    @Query("SELECT distinct new com.sequenceiq.cloudbreak.dto.SubnetIdWithResourceNameAndCrn(s.name, s.resourceCrn, im.subnetId, s.type) " +
            "FROM InstanceMetaData im " +
            "LEFT JOIN im.instanceGroup ig  " +
            "LEFT JOIN ig.stack s " +
            "WHERE s.environmentCrn = :environmentCrn " +
            "AND im.instanceStatus <> 'TERMINATED' ")
    List<SubnetIdWithResourceNameAndCrn> findAllUsedSubnetsByEnvironmentCrn(@Param("environmentCrn") String environmentCrn);

    @Modifying
    @Query("UPDATE InstanceMetaData SET publicIp = :publicIp WHERE instanceId = :instanceId")
    void updatePublicIp(@Param("instanceId") String instanceId, @Param("publicIp") String publicIp);

    @Query("SELECT i " +
            "FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "WHERE s.id= :stackId " +
            "AND i.instanceStatus in :status ")
    List<InstanceMetaData> findAllByStackIdAndStatus(@Param("stackId") Long stackId, @Param("status") InstanceStatus status);
}

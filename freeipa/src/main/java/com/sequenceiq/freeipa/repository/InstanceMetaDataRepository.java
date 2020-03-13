package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.BaseCrudRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.projection.StackAuthenticationView;

@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface InstanceMetaDataRepository extends BaseCrudRepository<InstanceMetaData, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Set<InstanceMetaData> findAllByInstanceIdIn(Iterable<String> instanceId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.instanceStatus <> 'TERMINATED'")
    Set<InstanceMetaData> findNotTerminatedForStack(@Param("stackId") Long stackId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId")
    Set<InstanceMetaData> findAllInStack(@Param("stackId") Long stackId);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceId = :instanceId AND i.instanceGroup.stack.id= :stackId")
    InstanceMetaData findByInstanceId(@Param("stackId") Long stackId, @Param("instanceId") String instanceId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s.id as stackId, s.stackAuthentication as stackAuthentication FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "WHERE i.id = :instanceId")
    Optional<StackAuthenticationView> getStackAuthenticationViewByInstanceMetaDataId(@Param("instanceId") Long id);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.discoveryFQDN= :hostName AND i.instanceStatus <> 'TERMINATED'")
    InstanceMetaData findHostInStack(@Param("stackId") Long stackId, @Param("hostName") String hostName);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId "
            + "AND i.instanceStatus in ('CREATED', 'UNREGISTERED', 'DECOMMISSIONED', 'FAILED', 'STOPPED')")
    Set<InstanceMetaData> findRemovableInstances(@Param("stackId") Long stackId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.privateIp= :privateAddress AND i.instanceStatus <> 'TERMINATED'")
    InstanceMetaData findNotTerminatedByPrivateAddress(@Param("stackId") Long stackId, @Param("privateAddress") String privateAddress);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT i.serverCert FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId AND i.instanceMetadataType = 'GATEWAY_PRIMARY' "
            + "AND i.instanceStatus <> 'TERMINATED'")
    String getServerCertByStackId(@Param("stackId") Long stackId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceMetadataType = 'GATEWAY_PRIMARY' AND i.instanceStatus <> 'TERMINATED' "
            + "AND i.instanceGroup.stack.id= :stackId")
    InstanceMetaData getPrimaryGatewayInstanceMetadata(@Param("stackId") Long stackId);

    @CheckPermission(action = ResourceAction.READ)
    List<InstanceMetaData> findAllByInstanceGroupAndInstanceStatus(InstanceGroup instanceGroup, InstanceStatus status);

}

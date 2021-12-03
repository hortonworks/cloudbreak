package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.projection.StackAuthenticationView;

@Transactional(TxType.REQUIRED)
public interface InstanceMetaDataRepository extends CrudRepository<InstanceMetaData, Long> {

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId " +
            "AND i.instanceStatus <> 'TERMINATED' " +
            "AND i.instanceStatus <> 'DELETED_ON_PROVIDER_SIDE' " +
            "AND i.instanceStatus <> 'DELETED_BY_PROVIDER'")
    Set<InstanceMetaData> findNotTerminatedForStack(@Param("stackId") Long stackId);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId")
    Set<InstanceMetaData> findAllInStack(@Param("stackId") Long stackId);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceId = :instanceId AND i.instanceGroup.stack.id= :stackId")
    InstanceMetaData findByInstanceId(@Param("stackId") Long stackId, @Param("instanceId") String instanceId);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceId IN :instanceIds AND i.instanceGroup.stack.id= :stackId")
    Set<InstanceMetaData> findAllByInstanceIdIn(@Param("stackId") Long stackId, @Param("instanceIds") Iterable<String> instanceIds);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceId IN :instanceIds AND i.instanceGroup.stack.id= :stackId AND i.instanceStatus <> 'TERMINATED'")
    Set<InstanceMetaData> findAllNotTerminatedByInstanceIdIn(@Param("stackId") Long stackId, @Param("instanceIds") Iterable<String> instanceIds);

    @Query("SELECT s.id as stackId, s.stackAuthentication as stackAuthentication FROM InstanceMetaData i " +
            "LEFT JOIN i.instanceGroup ig " +
            "LEFT JOIN ig.stack s " +
            "WHERE i.id = :instanceId")
    Optional<StackAuthenticationView> getStackAuthenticationViewByInstanceMetaDataId(@Param("instanceId") Long id);

    @Query("SELECT i FROM InstanceMetaData i WHERE i.instanceGroup.stack.id= :stackId "
            + "AND i.instanceStatus in ('CREATED', 'UNREGISTERED', 'DECOMMISSIONED', 'FAILED', 'STOPPED')")
    Set<InstanceMetaData> findRemovableInstances(@Param("stackId") Long stackId);

    List<InstanceMetaData> findAllByInstanceGroupAndInstanceStatus(InstanceGroup instanceGroup, InstanceStatus status);

}

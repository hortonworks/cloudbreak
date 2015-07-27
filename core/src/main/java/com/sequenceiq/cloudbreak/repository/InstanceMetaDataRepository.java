package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;

@EntityType(entityClass = InstanceMetaData.class)
public interface InstanceMetaDataRepository extends CrudRepository<InstanceMetaData, Long> {

    Set<InstanceMetaData> findNotTerminatedForStack(@Param("stackId") Long stackId);

    Set<InstanceMetaData> findAllInStack(@Param("stackId") Long stackId);

    InstanceMetaData findByInstanceId(@Param("stackId") Long stackId, @Param("instanceId") String instanceId);

    InstanceMetaData findHostInStack(@Param("stackId") Long stackId, @Param("hostName") String hostName);

    Set<InstanceMetaData> findUnregisteredHostsInInstanceGroup(@Param("instanceGroupId") Long instanceGroupId);

    List<String> findAliveInstancesHostNamesInInstanceGroup(@Param("instanceGroupId") Long instanceGroupId);

    Set<InstanceMetaData> findRemovableInstances(@Param("stackId") Long stackId, @Param("groupName") String groupName);

    InstanceMetaData findNotTerminatedByPrivateAddress(@Param("stackId") Long stackId, @Param("privateAddress") String privateAddress);

}

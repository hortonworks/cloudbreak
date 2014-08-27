package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;

public interface InstanceMetaDataRepository extends CrudRepository<InstanceMetaData, Long> {

    InstanceMetaData findHostInStack(@Param("stackId") Long stackId, @Param("hostName") String hostName);

    Set<InstanceMetaData> findUnregisteredHostsInStack(@Param("stackId") Long stackId);

}

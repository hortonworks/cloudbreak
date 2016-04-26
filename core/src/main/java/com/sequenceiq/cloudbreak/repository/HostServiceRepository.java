package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.HostService;

@EntityType(entityClass = HostService.class)
public interface HostServiceRepository extends CrudRepository<HostService, Long> {

    Set<HostService> findServicesInCluster(@Param("clusterId") Long clusterId);

}

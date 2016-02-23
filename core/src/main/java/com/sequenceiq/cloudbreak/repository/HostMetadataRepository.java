package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.HostMetadata;

@EntityType(entityClass = HostMetadata.class)
public interface HostMetadataRepository extends CrudRepository<HostMetadata, Long> {

    Set<HostMetadata> findHostsInCluster(@Param("clusterId") Long clusterId);

    Set<HostMetadata> findEmptyContainerHostsInHostGroup(@Param("hostGroupId") Long hostGroupId);

    HostMetadata findHostInClusterByName(@Param("clusterId") Long clusterId, @Param("hostName") String hostName);
}

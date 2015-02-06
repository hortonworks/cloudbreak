package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.HostMetadata;

public interface HostMetadataRepository extends CrudRepository<HostMetadata, Long> {

    Set<HostMetadata> findHostsInHostgroup(@Param("hostGroupId") Long hostGroupId);

    Set<HostMetadata> findHostsInCluster(@Param("clusterId") Long clusterId);

}

package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Cluster;

public interface ClusterRepository extends CrudRepository<Cluster, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    Cluster findOne(Long id);

}
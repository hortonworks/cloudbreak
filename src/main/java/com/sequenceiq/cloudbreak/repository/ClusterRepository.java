package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Cluster;

public interface ClusterRepository extends CrudRepository<Cluster, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    Cluster findOne(Long id);

    Cluster findById(Long id);

    Set<Cluster> findAllClusterByBlueprint(Long id);

}
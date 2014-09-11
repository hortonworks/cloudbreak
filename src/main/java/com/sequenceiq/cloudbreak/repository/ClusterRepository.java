package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Cluster;

public interface ClusterRepository extends CrudRepository<Cluster, Long> {

    @PostAuthorize("returnObject?.owner == principal")
    Cluster findOne(@Param("id") Long id);

    Cluster findById(@Param("id") Long id);

    Set<Cluster> findAllClusterByBlueprint(@Param("id") Long id);

    Cluster findOneWithLists(@Param("id") Long id);

}
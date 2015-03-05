package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Cluster;

public interface ClusterRepository extends CrudRepository<Cluster, Long> {

    Cluster findById(@Param("id") Long id);

    Set<Cluster> findAllClustersByBlueprint(@Param("id") Long id);

    Cluster findOneWithLists(@Param("id") Long id);

}
package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.periscope.domain.Cluster;

public interface ClusterRepository extends CrudRepository<Cluster, Long> {
}

package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Topology;

@EntityType(entityClass = Topology.class)
public interface TopologyRepository extends CrudRepository<Topology, Long> {
    Set<Topology> findAllInAccount(@Param("account") String account);

    Topology findByIdInAccount(@Param("id") Long id, @Param("account") String account);
}

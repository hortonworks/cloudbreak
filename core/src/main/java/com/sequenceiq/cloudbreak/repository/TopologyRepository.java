package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.Topology;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Set;

@EntityType(entityClass = Topology.class)
public interface TopologyRepository extends CrudRepository<Topology, Long> {

    @Query("SELECT t FROM Topology t WHERE t.account= :account AND deleted IS NOT TRUE")
    Set<Topology> findAllInAccount(@Param("account") String account);

    @Query("SELECT t FROM Topology t WHERE t.id= :id and t.account= :account AND deleted IS NOT TRUE")
    Topology findByIdInAccount(@Param("id") Long id, @Param("account") String account);
}

package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.common.type.Status;

@EntityType(entityClass = Cluster.class)
public interface ClusterRepository extends CrudRepository<Cluster, Long> {

    Cluster findById(@Param("id") Long id);

    Set<Cluster> findAllClustersByBlueprint(@Param("id") Long blueprintId);

    Cluster findOneWithLists(@Param("id") Long id);

    List<Cluster> findByStatus(@Param("status") Status status);

    Cluster findByNameInAccount(@Param("name") String name, @Param("account") String account);

}
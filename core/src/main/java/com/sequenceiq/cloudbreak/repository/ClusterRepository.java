package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.domain.Cluster;

@EntityType(entityClass = Cluster.class)
public interface ClusterRepository extends CrudRepository<Cluster, Long> {

    Cluster findById(@Param("id") Long id);

    Set<Cluster> findAllClustersByBlueprint(@Param("id") Long blueprintId);

    Set<Cluster> findAllClustersBySssdConfig(@Param("id") Long sssdConfigId);

    Cluster findOneWithLists(@Param("id") Long id);

    List<Cluster> findByStatuses(@Param("statuses") List<Status> statuses);

    Cluster findByNameInAccount(@Param("name") String name, @Param("account") String account);

    List<Cluster> findAllClustersForConstraintTemplate(@Param("id") Long id);

    Set<Cluster> findAllClustersByRDSConfig(@Param("id") Long rdsConfigId);

    Set<Cluster> findAllClustersByLdapConfig(@Param("id") Long ldapConfigId);
}
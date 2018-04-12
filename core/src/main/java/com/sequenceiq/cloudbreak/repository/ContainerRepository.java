package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.Container;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Set;

@EntityType(entityClass = Container.class)
public interface ContainerRepository extends CrudRepository<Container, Long> {

    @Query("SELECT c FROM Container c WHERE c.cluster.id= :clusterId")
    Set<Container> findContainersInCluster(@Param("clusterId") Long clusterId);

}

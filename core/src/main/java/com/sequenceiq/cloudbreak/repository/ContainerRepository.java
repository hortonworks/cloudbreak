package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Container;

@EntityType(entityClass = Container.class)
public interface ContainerRepository extends CrudRepository<Container, Long> {

    Set<Container> findContainersInCluster(@Param("clusterId") Long clusterId);

}

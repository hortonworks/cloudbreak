package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Container;

@EntityType(entityClass = Container.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ContainerRepository extends CrudRepository<Container, Long> {

    @Query("SELECT c FROM Container c WHERE c.cluster.id= :clusterId")
    Set<Container> findContainersInCluster(@Param("clusterId") Long clusterId);

}

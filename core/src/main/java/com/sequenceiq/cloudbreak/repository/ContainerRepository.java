package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Container.class)
@Transactional(TxType.REQUIRED)
public interface ContainerRepository extends CrudRepository<Container, Long> {

    @Query("SELECT c FROM Container c WHERE c.cluster.id= :clusterId")
    Set<Container> findContainersInCluster(@Param("clusterId") Long clusterId);

}

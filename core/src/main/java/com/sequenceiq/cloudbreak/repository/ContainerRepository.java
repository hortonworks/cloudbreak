package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.domain.Container;

@EntityType(entityClass = Container.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface ContainerRepository extends DisabledBaseRepository<Container, Long> {

    @Query("SELECT c FROM Container c WHERE c.cluster.id= :clusterId")
    Set<Container> findContainersInCluster(@Param("clusterId") Long clusterId);

}

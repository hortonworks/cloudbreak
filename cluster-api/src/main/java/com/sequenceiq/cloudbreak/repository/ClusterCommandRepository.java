package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommand;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@Repository
@Transactional(TxType.REQUIRED)
@EntityType(entityClass = ClusterCommand.class)
public interface ClusterCommandRepository extends CrudRepository<ClusterCommand, Long> {

    Optional<ClusterCommand> findTopByClusterIdAndClusterCommandType(long clusterId, ClusterCommandType clusterCommandType);

    @Modifying
    @Query(value = "DELETE FROM ClusterCommand c WHERE c.cluster_id = :clusterId", nativeQuery = true)
    void deleteByClusterId(@Param("clusterId") Long clusterId);
}

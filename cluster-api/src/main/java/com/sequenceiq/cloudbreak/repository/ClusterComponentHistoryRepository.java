package com.sequenceiq.cloudbreak.repository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponentHistory;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = ClusterComponentHistory.class)
public interface ClusterComponentHistoryRepository extends CrudRepository<ClusterComponentHistory, Long> {

    @Modifying
    @Query(value = "DELETE FROM clustercomponent_history c WHERE c.cluster_id = :clusterId", nativeQuery = true)
    void deleteByClusterId(@Param("clusterId") Long clusterId);

}

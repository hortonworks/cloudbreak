package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponentHistory;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = ClusterComponentHistory.class)
public interface ClusterComponentHistoryRepository extends CrudRepository<ClusterComponentHistory, Long> {

    @Modifying
    @Query(value = "WITH temRef AS (SELECT id FROM cluster WHERE cluster.status = 'DELETE_COMPLETED') " +
            "DELETE FROM clustercomponent_history cch " +
            "USING temRef " +
            "WHERE EXISTS (SELECT 1 FROM clustercomponent_history WHERE cch.cluster_id = temRef.id);",
            nativeQuery = true)
    void deleteByClusterIdIsNullOrClusterIdIsIn();

}
package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

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
    @Query(value = "DELETE FROM clustercomponent_history cch WHERE  cch.cluster_id IN (:clusterIds)", nativeQuery = true)
    void deleteByClusterIdIsNullOrClusterIdIsIn(@Param("clusterIds") Collection<Long> clusterIds);

}
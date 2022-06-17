package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import java.util.Collection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponentHistory;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import org.springframework.data.repository.CrudRepository;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = ClusterComponentHistory.class)
public interface ClusterComponentHistoryRepository extends CrudRepository<ClusterComponentHistory, Long> {

    long deleteByClusterIdIsNullOrClusterIdIn(Collection<Long> clusterIds);

}
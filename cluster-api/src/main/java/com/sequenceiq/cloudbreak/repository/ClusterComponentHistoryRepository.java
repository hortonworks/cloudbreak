package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import java.util.Collection;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponentHistoryView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import org.springframework.data.repository.CrudRepository;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = ClusterComponentHistoryView.class)
public interface ClusterComponentHistoryRepository extends CrudRepository<ClusterComponentHistoryView, Long> {

    long deleteByClusterIdIsNullOrClusterIdIn(Collection<Long> clusterIds);

}
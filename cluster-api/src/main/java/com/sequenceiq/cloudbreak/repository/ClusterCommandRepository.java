package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommand;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@Repository
@Transactional(TxType.REQUIRED)
@EntityType(entityClass = ClusterCommand.class)
public interface ClusterCommandRepository extends CrudRepository<ClusterCommand, Long> {

    Optional<ClusterCommand> findTopByClusterIdAndClusterCommandType(long clusterId, ClusterCommandType clusterCommandType);
}

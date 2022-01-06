package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterBase;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@Repository
@EntityType(entityClass = ClusterBase.class)
public interface ClusterBaseRepository extends CrudRepository<ClusterBase, Long> {
}

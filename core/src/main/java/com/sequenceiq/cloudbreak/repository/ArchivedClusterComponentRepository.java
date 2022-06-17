package com.sequenceiq.cloudbreak.repository;

import static javax.transaction.Transactional.TxType.REQUIRED;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ArchivedClusterComponent;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import org.springframework.data.repository.CrudRepository;

@Transactional(REQUIRED)
@EntityType(entityClass = ArchivedClusterComponent.class)
public interface ArchivedClusterComponentRepository extends CrudRepository<ArchivedClusterComponent, Long> {

}

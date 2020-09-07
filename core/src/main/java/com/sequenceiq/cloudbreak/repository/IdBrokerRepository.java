package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = IdBroker.class)
@Transactional(TxType.REQUIRED)
public interface IdBrokerRepository extends CrudRepository<IdBroker, Long> {
}

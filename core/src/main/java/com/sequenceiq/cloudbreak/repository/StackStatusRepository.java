package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.stack.StackStatus;

@EntityType(entityClass = StackStatus.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface StackStatusRepository extends CrudRepository<StackStatus, Long> {

    StackStatus findFirstByStackIdOrderByCreatedDesc(long stackId);
}

package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = StackStatus.class)
@Transactional(TxType.REQUIRED)
public interface StackStatusRepository extends CrudRepository<StackStatus, Long> {

    Optional<StackStatus> findFirstByStackIdOrderByCreatedDesc(long stackId);

    List<StackStatus> findAllByStackIdOrderByCreatedAsc(long stackId);

}

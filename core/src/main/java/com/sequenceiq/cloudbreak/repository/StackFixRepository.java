package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackFix;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = StackFix.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface StackFixRepository extends JpaRepository<StackFix, Long> {

    Optional<StackFix> findByStackAndType(Stack stack, StackFix.StackFixType stackFixType);
}

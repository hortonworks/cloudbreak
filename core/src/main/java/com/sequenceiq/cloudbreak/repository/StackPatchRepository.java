package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.domain.stack.StackPatch;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = StackPatch.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface StackPatchRepository extends JpaRepository<StackPatch, Long> {

    Optional<StackPatch> findByStackIdAndType(Long stackId, StackPatchType stackPatchType);

    List<StackPatch> findByTypeAndStackIdIn(StackPatchType stackPatchType, Collection<Long> stackIds);
}

package com.sequenceiq.freeipa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.sequenceiq.freeipa.entity.StackPatch;
import com.sequenceiq.freeipa.entity.StackPatchType;

@Transactional(Transactional.TxType.REQUIRED)
public interface StackPatchRepository extends JpaRepository<StackPatch, Long> {

    Optional<StackPatch> findByStackIdAndType(Long stackId, StackPatchType stackPatchType);

    List<StackPatch> findByTypeAndStackIdIn(StackPatchType stackPatchType, Collection<Long> stackIds);

    List<StackPatch> findByTypeIn(Set<StackPatchType> stackPatchTypes);

    @Modifying
    @Query("DELETE FROM StackPatch sp WHERE sp.stackId = :stackId")
    void deleteByStackId(Long stackId);
}

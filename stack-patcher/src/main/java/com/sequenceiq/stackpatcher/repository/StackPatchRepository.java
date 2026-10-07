package com.sequenceiq.stackpatcher.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.stackpatcher.domain.StackPatch;
import com.sequenceiq.stackpatcher.domain.StackPatchType;

@EntityType(entityClass = StackPatch.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface StackPatchRepository extends JpaRepository<StackPatch, Long> {

    Optional<StackPatch> findByResourceIdAndType(Long resourceId, StackPatchType stackPatchType);

    List<StackPatch> findByTypeAndResourceIdIn(StackPatchType stackPatchType, Collection<Long> resourceIds);

    List<StackPatch> findByTypeIn(Set<StackPatchType> stackPatchTypes);

    @Modifying
    @Query("DELETE FROM StackPatch se WHERE se.resourceId = :resourceId")
    void deleteByResourceId(Long resourceId);
}

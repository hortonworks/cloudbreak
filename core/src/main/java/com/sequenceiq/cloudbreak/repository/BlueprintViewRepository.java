package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = BlueprintView.class)
@Transactional(TxType.REQUIRED)
public interface BlueprintViewRepository extends WorkspaceResourceRepository<BlueprintView, Long> {

    @Query("SELECT b FROM BlueprintView b WHERE b.workspace.id= :workspaceId AND b.status <> 'DEFAULT_DELETED'")
    Set<BlueprintView> findAllByNotDeletedInWorkspace(@Param("workspaceId") Long workspaceId);
}

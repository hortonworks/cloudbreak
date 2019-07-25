package com.sequenceiq.cloudbreak.structuredevent.db;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspace;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.workspace.repository.check.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.workspace.repository.check.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;

@EntityType(entityClass = StructuredEventEntity.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@WorkspaceResourceType(resource = WorkspaceResource.STRUCTURED_EVENT)
public interface StructuredEventRepository extends WorkspaceResourceRepository<StructuredEventEntity, Long> {

    @Override
    @DisableCheckPermissions
    StructuredEventEntity save(StructuredEventEntity entity);

    @CheckPermissionsByWorkspaceId
    @Query("SELECT se from StructuredEventEntity se WHERE se.workspace.id = :workspaceId AND se.id = :id")
    StructuredEventEntity findByWorkspaceIdAndId(@Param("workspaceId") Long workspaceId, @Param("id") Long id);

    @CheckPermissionsByWorkspace(workspaceIndex = 0)
    List<StructuredEventEntity> findByWorkspaceAndResourceTypeAndResourceId(Workspace workspace, String resourceType, Long resourceId);

    @CheckPermissionsByWorkspace(workspaceIndex = 0)
    List<StructuredEventEntity> findByWorkspaceAndResourceTypeAndResourceCrn(Workspace workspace, String resourceType, String resourceCrn);

    @CheckPermissionsByWorkspace(workspaceIndex = 0)
    List<StructuredEventEntity> findByWorkspaceAndEventType(Workspace workspace, StructuredEventType eventType);

    @CheckPermissionsByWorkspaceId
    @Query("SELECT se from StructuredEventEntity se WHERE se.workspace.id = :workspaceId AND se.eventType = :eventType AND se.timestamp >= :since")
    List<StructuredEventEntity> findByWorkspaceIdAndEventTypeSince(@Param("workspaceId") Long workspaceId, @Param("eventType") StructuredEventType eventType,
            @Param("since") Long since);

    @CheckPermissionsByReturnValue
    List<StructuredEventEntity> findByEventTypeAndResourceTypeAndResourceId(StructuredEventType eventType, String resourceType, Long resourceId);

    @DisableCheckPermissions
    @Query("SELECT se from StructuredEventEntity se WHERE se.workspace = null OR se.user = null")
    List<StructuredEventEntity> findAllWithoutWorkspaceOrUser();

    @Override
    default Optional<StructuredEventEntity> findByNameAndWorkspace(String name, Workspace workspace) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Optional<StructuredEventEntity> findByNameAndWorkspaceId(String name, Long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Set<StructuredEventEntity> findByNameInAndWorkspaceId(Set<String> name, Long workspaceId) {
        throw new UnsupportedOperationException();
    }
}

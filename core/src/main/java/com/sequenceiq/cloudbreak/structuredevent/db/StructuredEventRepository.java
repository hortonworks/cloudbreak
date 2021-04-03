package com.sequenceiq.cloudbreak.structuredevent.db;

import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspace;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.aspect.workspace.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
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
    Page<StructuredEventEntity> findByWorkspaceAndResourceTypeAndResourceId(Workspace workspace, String resourceType, Long resourceId, Pageable pageable);

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
    default StructuredEventEntity findByNameAndWorkspace(String name, Workspace workspace) {
        throw new UnsupportedOperationException();
    }

    @Override
    default StructuredEventEntity findByNameAndWorkspaceId(String name, Long workspaceId) {
        throw new UnsupportedOperationException();
    }
}

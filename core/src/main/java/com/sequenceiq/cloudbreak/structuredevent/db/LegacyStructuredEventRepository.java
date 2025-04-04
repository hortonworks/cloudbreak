package com.sequenceiq.cloudbreak.structuredevent.db;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = StructuredEventEntity.class)
@Transactional(TxType.REQUIRED)
public interface LegacyStructuredEventRepository extends WorkspaceResourceRepository<StructuredEventEntity, Long> {

    @Override
    StructuredEventEntity save(StructuredEventEntity entity);

    @Query("SELECT se from StructuredEventEntity se WHERE se.workspace.id = :workspaceId AND se.id = :id")
    StructuredEventEntity findByWorkspaceIdAndId(@Param("workspaceId") Long workspaceId, @Param("id") Long id);

    Page<StructuredEventEntity> findByWorkspaceAndResourceTypeAndResourceId(Workspace workspace, String resourceType, Long resourceId, Pageable page);

    Page<StructuredEventEntity> findByWorkspaceAndResourceCrn(Workspace workspace, String resourceCrn, Pageable page);

    Page<StructuredEventEntity> findByWorkspaceAndEventType(Workspace workspace, StructuredEventType eventType, Pageable page);

    @Query("SELECT se from StructuredEventEntity se WHERE se.workspace.id = :workspaceId AND se.eventType = :eventType AND se.timestamp >= :since")
    List<StructuredEventEntity> findByWorkspaceIdAndEventTypeSince(@Param("workspaceId") Long workspaceId, @Param("eventType") StructuredEventType eventType,
            @Param("since") Long since);

    Page<StructuredEventEntity> findByEventTypeAndResourceTypeAndResourceId(StructuredEventType eventType, String resourceType, Long resourceId, Pageable page);

    @Query(value = "SELECT se FROM StructuredEventEntity se WHERE se.eventType = :eventType" +
            " AND se.resourceType = :resourceType AND se.resourceId = :resourceId ORDER BY se.timestamp DESC LIMIT :limit")
    List<StructuredEventEntity> findLastEventsByEventTypeAndResourceTypeAndResourceId(@Param("eventType") StructuredEventType eventType,
            @Param("resourceType") String resourceType, @Param("resourceId") Long resourceId, @Param("limit") Integer limit);

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

    @Modifying
    @Query("DELETE FROM StructuredEventEntity se WHERE se.resourceId = :resourceId AND se.timestamp <= :timestamp")
    void deleteRecordsByResourceIdOlderThan(@Param("resourceId") Long resourceid, @Param("timestamp") Long timestamp);

    @Modifying
    @Query("DELETE FROM StructuredEventEntity se WHERE se.resourceCrn = :resourceCrn")
    void deleteByResourceCrn(String resourceCrn);
}

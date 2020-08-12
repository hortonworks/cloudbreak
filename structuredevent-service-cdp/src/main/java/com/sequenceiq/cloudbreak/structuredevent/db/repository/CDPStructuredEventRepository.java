package com.sequenceiq.cloudbreak.structuredevent.db.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.structuredevent.db.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = CDPStructuredEventEntity.class)
@Transactional(TxType.REQUIRED)
public interface CDPStructuredEventRepository extends AccountAwareResourceRepository<CDPStructuredEventEntity, Long> {

    @Override
    CDPStructuredEventEntity save(CDPStructuredEventEntity entity);

    @Query("SELECT se from CDPStructuredEventEntity se WHERE se.accountId = :accountId AND se.id = :id")
    CDPStructuredEventEntity findByAccountIdAndId(
            @Param("accountId") Long accountId,
            @Param("id") Long id);

    List<CDPStructuredEventEntity> findByAccountIdAndResourceTypeAndResourceId(
            String accountId,
            String resourceType,
            Long resourceId);

    List<CDPStructuredEventEntity> findByAccountIdAndResourceTypeAndResourceCrn(
            String accountId,
            String resourceType,
            String resourceCrn);

    List<CDPStructuredEventEntity> findByAccountIdAndEventType(
            String accountId,
            StructuredEventType eventType);

    @Query("SELECT se from CDPStructuredEventEntity se WHERE se.accountId = :accountId AND se.eventType = :eventType AND se.timestamp >= :since")
    List<CDPStructuredEventEntity> findByAccountIdAndEventTypeSince(
            @Param("accountId") String accountId,
            @Param("eventType") StructuredEventType eventType,
            @Param("since") Long since);

    List<CDPStructuredEventEntity> findByEventTypeAndResourceTypeAndResourceId(
            StructuredEventType eventType,
            String resourceType,
            Long resourceId);

    @Override
    default Optional<CDPStructuredEventEntity> findByNameAndAccountId(
            String name,
            String accountId) {
        throw new UnsupportedOperationException();
    }
}

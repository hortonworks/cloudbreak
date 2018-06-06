package com.sequenceiq.cloudbreak.structuredevent.db;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.repository.EntityType;

@EntityType(entityClass = StructuredEventEntity.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface StructuredEventRepository extends CrudRepository<StructuredEventEntity, Long> {
    List<StructuredEventEntity> findByUserIdAndEventType(String userId, String eventType);

    List<StructuredEventEntity> findByUserIdAndEventTypeAndResourceTypeAndResourceId(String userId, String eventType, String resourceType, Long resourceId);

    @Query("SELECT se from StructuredEventEntity se WHERE se.userId = :userId AND se.eventType = :eventType AND se.timestamp >= :since")
    List<StructuredEventEntity> findByUserIdAndEventTypeSince(@Param("userId") String userId, @Param("eventType") String eventType, @Param("since") Long since);
}

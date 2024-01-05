package com.sequenceiq.cloudbreak.structuredevent.db;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = StructuredEventEntity.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface LegacyPagingStructuredEventRepository extends PagingAndSortingRepository<StructuredEventEntity, Long>,
        CrudRepository<StructuredEventEntity, Long> {

    Page<StructuredEventEntity> findByEventTypeAndResourceTypeAndResourceId(StructuredEventType eventType, String resourceType, Long resourceId,
            Pageable pageable);
}

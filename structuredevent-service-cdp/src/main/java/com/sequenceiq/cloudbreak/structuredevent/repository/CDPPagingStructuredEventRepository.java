package com.sequenceiq.cloudbreak.structuredevent.repository;

import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = CDPStructuredEventEntity.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface CDPPagingStructuredEventRepository extends PagingAndSortingRepository<CDPStructuredEventEntity, Long> {

    Page<CDPStructuredEventEntity> findByEventTypeAndResourceTypeAndResourceCrn(StructuredEventType eventType,
        String resourceType,
        String resourceCrn,
        Pageable pageable);
}
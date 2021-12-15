package com.sequenceiq.cloudbreak.structuredevent.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = CDPStructuredEventEntity.class)
@Transactional(Transactional.TxType.REQUIRED)
@Repository
public interface CDPPagingStructuredEventRepository extends PagingAndSortingRepository<CDPStructuredEventEntity, Long> {

    Page<CDPStructuredEventEntity> findByEventTypeAndResourceCrn(StructuredEventType eventType, String resourceCrn, Pageable pageable);

    Page<CDPStructuredEventEntity> findByEventTypeInAndResourceCrn(List<StructuredEventType> eventType, String resourceCrn, Pageable pageable);

    Page<CDPStructuredEventEntity> findByEventTypeInAndResourceCrnIn(List<StructuredEventType> eventType, List<String> resourceCrn, Pageable pageable);
}
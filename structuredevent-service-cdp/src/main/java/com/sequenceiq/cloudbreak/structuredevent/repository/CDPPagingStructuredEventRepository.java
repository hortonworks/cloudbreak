package com.sequenceiq.cloudbreak.structuredevent.repository;

import java.util.List;

import jakarta.persistence.QueryHint;
import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = CDPStructuredEventEntity.class)
@Transactional(Transactional.TxType.REQUIRED)
@Repository
public interface CDPPagingStructuredEventRepository extends PagingAndSortingRepository<CDPStructuredEventEntity, Long>,
        CrudRepository<CDPStructuredEventEntity, Long> {

    Page<CDPStructuredEventEntity> findByEventTypeAndResourceCrn(StructuredEventType eventType, String resourceCrn, Pageable pageable);

    @QueryHints(@QueryHint(name = "jakarta.persistence.query.timeout", value = "65000"))
    @Query("SELECT e FROM CDPStructuredEventEntity e WHERE e.eventType IN :eventType AND e.resourceCrn = :resourceCrn")
    Slice<CDPStructuredEventEntity> findByEventTypeInAndResourceCrn(
            @Param("eventType") List<StructuredEventType> eventType,
            @Param("resourceCrn") String resourceCrn,
            Pageable pageable);

    @QueryHints(@QueryHint(name = "jakarta.persistence.query.timeout", value = "65000"))
    @Query("SELECT e FROM CDPStructuredEventEntity e WHERE e.eventType IN :eventType AND e.resourceCrn IN :resourceCrn")
    Slice<CDPStructuredEventEntity> findByEventTypeInAndResourceCrnIn(
            @Param("eventType") List<StructuredEventType> eventType,
            @Param("resourceCrn") List<String> resourceCrn,
            Pageable pageable);
}

package com.sequenceiq.cloudbreak.structuredevent.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = CDPStructuredEventEntity.class)
@Transactional(TxType.REQUIRED)
@Repository
public interface CDPStructuredEventRepository extends AccountAwareResourceRepository<CDPStructuredEventEntity, Long> {

    @Override
    CDPStructuredEventEntity save(CDPStructuredEventEntity entity);

    List<CDPStructuredEventEntity> findByEventTypeInAndResourceCrn(List<StructuredEventType> eventTypes, String resourceCrn);

    List<CDPStructuredEventEntity> findByEventTypeInAndResourceCrnIn(List<StructuredEventType> eventType, List<String> resourceCrn);

    @Override
    default Optional<CDPStructuredEventEntity> findByNameAndAccountId(
            String name,
            String accountId) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Set<CDPStructuredEventEntity> findByNameInAndAccountId(Set<String> name, String accountId) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Optional<CDPStructuredEventEntity> findByResourceCrnAndAccountId(
            String crn,
            String accountId) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Set<CDPStructuredEventEntity> findByResourceCrnInAndAccountId(Set<String> crn, String accountId) {
        throw new UnsupportedOperationException();
    }

    default Optional<ResourceBasicView> findResourceBasicViewByResourceCrn(String resourceCrn) {
        throw new UnsupportedOperationException();
    }

    default List<ResourceBasicView> findAllResourceBasicViewByResourceCrns(Collection<String> resourceCrns) {
        throw new UnsupportedOperationException();
    }

    default Optional<ResourceBasicView> findResourceBasicViewByNameAndAccountId(String name, String accountId) {
        throw new UnsupportedOperationException();
    }

    default List<ResourceBasicView> findAllResourceBasicViewByNamesAndAccountId(Collection<String> names, String accountId) {
        throw new UnsupportedOperationException();
    }

    @Modifying
    @Query("DELETE FROM CDPStructuredEventEntity cdse WHERE cdse.accountId = :accountId AND cdse.timestamp <= :timestamp")
    void deleteByAccountIdOlderThan(@Param("accountId") String accountId, @Param("timestamp") Long timestamp);

    @Modifying
    @Query("DELETE FROM CDPStructuredEventEntity cdse WHERE cdse.resourceCrn = :resourceCrn AND cdse.timestamp <= :timestamp")
    void deleteByResourceCrnOlderThan(@Param("resourceCrn") String resourceCrn, @Param("timestamp") Long timestamp);

    @Modifying
    void deleteByResourceCrn(String resourceCrn);
}

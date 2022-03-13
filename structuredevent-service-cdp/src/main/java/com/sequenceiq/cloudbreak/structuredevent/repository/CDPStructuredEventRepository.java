package com.sequenceiq.cloudbreak.structuredevent.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
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

}

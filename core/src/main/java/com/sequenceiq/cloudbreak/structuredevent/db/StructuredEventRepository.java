package com.sequenceiq.cloudbreak.structuredevent.db;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;

@EntityType(entityClass = StructuredEventEntity.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
@OrganizationResourceType(resource = OrganizationResource.STRUCTURED_EVENT)
public interface StructuredEventRepository extends OrganizationResourceRepository<StructuredEventEntity, Long> {

    @Override
    @DisableCheckPermissions
    StructuredEventEntity save(StructuredEventEntity entity);

    @Override
    @DisableCheckPermissions
    Optional<StructuredEventEntity> findById(Long id);

    @DisableCheckPermissions
    List<StructuredEventEntity> findByOrganizationAndEventType(Organization organization, StructuredEventType eventType);

    @DisableCheckPermissions
    List<StructuredEventEntity> findByOrganizationAndResourceTypeAndResourceId(Organization organization, String resourceType, Long resourceId);

    @DisableCheckPermissions
    List<StructuredEventEntity> findByOrganizationAndEventTypeAndResourceTypeAndResourceId(Organization organization, StructuredEventType eventType,
            String resourceType, Long resourceId);

    @DisableCheckPermissions
    @Query("SELECT se from StructuredEventEntity se WHERE se.organization.id = :orgId AND se.eventType = :eventType AND se.timestamp >= :since")
    List<StructuredEventEntity> findByOrgIdAndEventTypeSince(@Param("orgId") Long organizationId, @Param("eventType") StructuredEventType eventType,
            @Param("since") Long since);

    @DisableCheckPermissions
    @Query("SELECT se from StructuredEventEntity se WHERE se.organization.id = :orgId AND se.id = :id")
    StructuredEventEntity findByOrgIdAndId(@Param("orgId") Long orgId, @Param("id") Long id);

    @Override
    default StructuredEventEntity findByNameAndOrganization(String name, Organization organization) {
        throw new UnsupportedOperationException();
    }

    @Override
    default StructuredEventEntity findByNameAndOrganizationId(String name, Long organizationId) {
        throw new UnsupportedOperationException();
    }
}

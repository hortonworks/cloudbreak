package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Blueprint.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@OrganizationResourceType(resource = OrganizationResource.BLUEPRINT)
public interface BlueprintRepository extends OrganizationResourceRepository<Blueprint, Long> {

    @Query("SELECT b FROM Blueprint b WHERE b.organization.id= :organizationId AND b.status <> 'DEFAULT_DELETED'")
    @CheckPermissionsByReturnValue
    Set<Blueprint> findAllByNotDeletedInOrganization(@Param("organizationId") long organizationId);
}
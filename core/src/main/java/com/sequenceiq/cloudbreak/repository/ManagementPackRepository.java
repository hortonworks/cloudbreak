package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = ManagementPack.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@OrganizationResourceType(resource = OrganizationResource.MPACK)
public interface ManagementPackRepository extends OrganizationResourceRepository<ManagementPack, Long> {

}

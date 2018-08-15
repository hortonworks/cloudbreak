package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action.READ;

import java.io.Serializable;
import java.util.Set;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganization;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganizationId;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;

@NoRepositoryBean
@DisableHasPermission
public interface OrganizationResourceRepository<T extends OrganizationAwareResource, ID extends Serializable> extends BaseRepository<T, ID> {

    @CheckPermissionsByOrganization(action = READ, organizationIndex = 0)
    Set<T> findAllByOrganization(Organization organization);

    @CheckPermissionsByOrganization(action = READ, organizationIndex = 1)
    T findByNameAndOrganization(String name, Organization organization);

    @CheckPermissionsByOrganizationId(action = READ)
    Set<T> findAllByOrganizationId(@Param("organizationId") Long organizationId);
}

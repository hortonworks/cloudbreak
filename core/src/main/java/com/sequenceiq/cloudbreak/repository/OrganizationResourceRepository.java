package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action.READ;

import java.io.Serializable;
import java.util.Set;

import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganization;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;

@NoRepositoryBean
@DisableHasPermission
public interface OrganizationResourceRepository<T extends OrganizationAwareResource, ID extends Serializable> extends BaseRepository<T, ID> {

    @CheckPermissionsByOrganization(action = READ, organizationIndex = 0)
    Set<T> findByOrganization(Organization organization);

    @CheckPermissionsByOrganization(action = READ, organizationIndex = 1)
    T findByNameAndOrganization(String name, Organization organization);
}

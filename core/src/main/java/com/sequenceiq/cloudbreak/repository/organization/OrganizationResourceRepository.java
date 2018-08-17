package com.sequenceiq.cloudbreak.repository.organization;

import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action.READ;

import java.io.Serializable;
import java.util.Set;

import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganization;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganizationId;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;
import com.sequenceiq.cloudbreak.repository.BaseRepository;

@NoRepositoryBean
@DisableHasPermission
public interface OrganizationResourceRepository<T extends OrganizationAwareResource, ID extends Serializable> extends BaseRepository<T, ID> {

    @CheckPermissionsByOrganization(action = READ, organizationIndex = 0)
    Set<T> findAllByOrganization(Organization organization);

    @CheckPermissionsByOrganizationId(action = READ)
    Set<T> findAllByOrganizationId(Long organizationId);

    @CheckPermissionsByOrganization(action = READ, organizationIndex = 1)
    T findByNameAndOrganization(String name, Organization organization);

    @CheckPermissionsByOrganizationId(action = READ, organizationIdIndex = 1)
    T findByNameAndOrganizationId(String name, Long organizationId);
}

package com.sequenceiq.cloudbreak.service.organization;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.OrganizationResource;

public interface OrganizationResourceService<T extends OrganizationResource> {

    T create(IdentityUser identityUser, T resource);

    T create(IdentityUser identityUser, T resource, Long organizationId);

    T getByNameForOrganization(String name, Long organizationId);

    T getByNameForOrganization(String name, Organization organization);

    T getByNameFromUsersDefaultOrganization(String name);

    Set<T> listByOrganization(Long organizationId);

    Set<T> listByOrganization(Organization organization);

    Set<T> listForUsersDefaultOrganization();

    T delete(T resource);

    T deleteByNameFromOrganization(String name, Long organizationId);

    T deleteByNameFromDefaultOrganization(String name);
}

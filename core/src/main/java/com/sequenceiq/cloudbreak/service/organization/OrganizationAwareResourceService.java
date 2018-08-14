package com.sequenceiq.cloudbreak.service.organization;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;

public interface OrganizationAwareResourceService<T extends OrganizationAwareResource> {

    T createInDefaultOrganization(T resource);

    T create(T resource, Long organizationId);

    T getByNameForOrganizationId(String name, Long organizationId);

    T getByNameForOrganization(String name, Organization organization);

    T getByNameFromUsersDefaultOrganization(String name);

    Set<T> findAllByOrganization(Organization organization);

    Set<T> findAllByOrganizationId(Long organizationId);

    Set<T> findAllForUsersDefaultOrganization();

    T delete(T resource);

    T deleteByNameFromOrganization(String name, Long organizationId);

    T deleteByNameFromDefaultOrganization(String name);
}

package com.sequenceiq.cloudbreak.service.organization;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;

public interface OrganizationAwareResourceService<T extends OrganizationAwareResource> {

    T create(T resource, Long organizationId);

    T create(T resource, Organization organization);

    T getByNameForOrganizationId(String name, Long organizationId);

    T getByNameForOrganization(String name, Organization organization);

    Set<T> findAllByOrganization(Organization organization);

    Set<T> findAllByOrganizationId(Long organizationId);

    T delete(T resource);

    T deleteByNameFromOrganization(String name, Long organizationId);

    T update(T resource);

    T updateInOrganization(Long organizationId, T resource);

    T getByIdFromAnyAvailableOrganization(Long id);

    T deleteByIdFromAnyAvailableOrganization(Long id);
}

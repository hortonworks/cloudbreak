package com.sequenceiq.cloudbreak.service.organization;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;

public interface LegacyOrganizationAwareResourceService<T extends OrganizationAwareResource> extends OrganizationAwareResourceService<T> {

    T createInDefaultOrganization(T resource);

    T getByNameFromUsersDefaultOrganization(String name);

    Set<T> findAllForUsersDefaultOrganization();

    T deleteByNameFromDefaultOrganization(String name);
}

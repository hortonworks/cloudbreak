package com.sequenceiq.cloudbreak.domain.security;


import com.sequenceiq.cloudbreak.validation.OrganizationPermissions.Resource;

public interface OrganizationAwareResource {

    Organization getOrganization();

    String getName();

    void setOrganization(Organization organization);

    Resource getResource();
}

package com.sequenceiq.cloudbreak.domain.security;


import com.sequenceiq.cloudbreak.validation.OrganizationPermissions.Resource;

public interface OrganizationResource {

    Organization getOrganization();

    void setOrganization(Organization organization);

    Resource getResource();
}

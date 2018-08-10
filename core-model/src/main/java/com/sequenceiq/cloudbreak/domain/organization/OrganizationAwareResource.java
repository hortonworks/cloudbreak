package com.sequenceiq.cloudbreak.domain.organization;


import com.sequenceiq.cloudbreak.authorization.OrganizationResource;

public interface OrganizationAwareResource {

    Organization getOrganization();

    String getName();

    void setOrganization(Organization organization);

    OrganizationResource getResource();
}

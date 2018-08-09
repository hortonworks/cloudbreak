package com.sequenceiq.cloudbreak.converter.users;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.OrganizationResourceResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.security.Organization;

@Component
public class OrganizationToOrganizationResourceResponseConverter extends AbstractConversionServiceAwareConverter<Organization, OrganizationResourceResponse> {

    @Override
    public OrganizationResourceResponse convert(Organization source) {
        OrganizationResourceResponse organization = new OrganizationResourceResponse();
        organization.setName(source.getName());
        organization.setId(source.getId());
        return organization;
    }
}

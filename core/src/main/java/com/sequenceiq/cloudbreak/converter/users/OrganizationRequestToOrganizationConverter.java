package com.sequenceiq.cloudbreak.converter.users;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.OrganizationRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class OrganizationRequestToOrganizationConverter extends AbstractConversionServiceAwareConverter<OrganizationRequest, Organization> {

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private UserService userService;

    @Override
    public Organization convert(OrganizationRequest json) {
        User user = userService.getOrCreate(authenticatedUserService.getCbUser());

        Organization organization = new Organization();
        organization.setName(json.getName());
        organization.setDescription(json.getDescription());
        organization.setTenant(user.getTenant());

        return organization;
    }
}

package com.sequenceiq.cloudbreak.converter.users;

import static com.sequenceiq.cloudbreak.api.model.v2.OrganizationStatus.ACTIVE;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.OrganizationRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class OrganizationRequestToOrganizationConverter extends AbstractConversionServiceAwareConverter<OrganizationRequest, Organization> {

    @Inject
    private UserService userService;

    @Override
    public Organization convert(OrganizationRequest json) {
        User user = userService.getCurrentUser();
        Organization organization = new Organization();
        organization.setName(json.getName());
        organization.setDescription(json.getDescription());
        organization.setTenant(user.getTenant());
        organization.setStatus(ACTIVE);
        return organization;
    }
}

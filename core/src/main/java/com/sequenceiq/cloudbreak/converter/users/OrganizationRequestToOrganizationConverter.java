package com.sequenceiq.cloudbreak.converter.users;

import static com.sequenceiq.cloudbreak.api.model.v2.OrganizationStatus.ACTIVE;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.OrganizationRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class OrganizationRequestToOrganizationConverter extends AbstractConversionServiceAwareConverter<OrganizationRequest, Organization> {

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public Organization convert(OrganizationRequest source) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = new Organization();
        organization.setName(source.getName());
        organization.setDescription(source.getDescription());
        organization.setTenant(user.getTenant());
        organization.setStatus(ACTIVE);
        return organization;
    }
}

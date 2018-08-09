package com.sequenceiq.cloudbreak.converter.users;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.OrganizationResponse;
import com.sequenceiq.cloudbreak.api.model.users.UserOrgPermissionsJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.UserOrgPermissions;
import com.sequenceiq.cloudbreak.service.user.UserOrgPermissionsService;

@Component
public class OrganizationToOrganizationResponseConverter extends AbstractConversionServiceAwareConverter<Organization, OrganizationResponse> {

    @Inject
    private UserOrgPermissionsService userOrgPermissionService;

    @Override
    public OrganizationResponse convert(Organization organization) {
        OrganizationResponse json = new OrganizationResponse();
        json.setDescription(organization.getDescription());
        json.setName(organization.getName());
        json.setId(organization.getId());
        json.setStatus(organization.getStatus());
        Set<UserOrgPermissions> userPermissions = userOrgPermissionService.findForOrganization(organization);
        json.setUsers((Set<UserOrgPermissionsJson>) getConversionService().convert(userPermissions, TypeDescriptor.forObject(userPermissions),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(UserOrgPermissionsJson.class))));
        return json;
    }
}

package com.sequenceiq.cloudbreak.converter;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.OrganizationResponse;
import com.sequenceiq.cloudbreak.api.model.UserOrgPermissionsJson;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.UserOrgPermissions;
import com.sequenceiq.cloudbreak.repository.security.UserOrgPermissionsRepository;

@Component
public class OrganizationToOrganizationResponseConverter extends AbstractConversionServiceAwareConverter<Organization, OrganizationResponse> {

    @Inject
    private UserOrgPermissionsRepository userOrgPermissionsRepository;

    @Override
    public OrganizationResponse convert(Organization organization) {
        OrganizationResponse json = new OrganizationResponse();
        json.setDescription(organization.getDescription());
        json.setName(organization.getName());
        json.setId(organization.getId());
        Set<UserOrgPermissions> userPermissions = userOrgPermissionsRepository.findForOrganization(organization);
        json.setUsers((Set<UserOrgPermissionsJson>) getConversionService().convert(userPermissions, TypeDescriptor.forObject(userPermissions),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(UserOrgPermissionsJson.class))));
        return json;
    }
}

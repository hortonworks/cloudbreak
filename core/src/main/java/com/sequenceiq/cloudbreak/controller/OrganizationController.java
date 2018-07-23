package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.OrganizationEndpoint;
import com.sequenceiq.cloudbreak.api.model.ChangeOrganizationUsersJson;
import com.sequenceiq.cloudbreak.api.model.OrganizationRequest;
import com.sequenceiq.cloudbreak.api.model.OrganizationResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

@Component
@Transactional(TxType.NEVER)
public class OrganizationController extends NotificationController implements OrganizationEndpoint {

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public OrganizationResponse post(@Valid OrganizationRequest organizationRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createOrganization(user, organizationRequest);
    }

    @Override
    public Set<OrganizationResponse> getAll() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<Organization> recipes = organizationService.retrieveForUser(user);
        return toJsonSet(recipes);
    }

    @Override
    public OrganizationResponse getByName(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Organization organization = organizationService.getByName(name, user);
        return conversionService.convert(organization, OrganizationResponse.class);
    }

    @Override
    public OrganizationResponse get(Long id) {
        Organization organization = organizationService.get(id);
        return conversionService.convert(organization, OrganizationResponse.class);
    }

    @Override
    public void delete(Long id) {
        executeAndNotify(user -> organizationService.delete(id), ResourceEvent.ORGANIZATION_DELETED);
    }

    @Override
    public void deleteByName(String name) {

    }

    @Override
    public void changeUsers(Long id, Set<ChangeOrganizationUsersJson> changeOrganizationUsersJson) {
        organizationService.changeUsers(id, jsonToMap(changeOrganizationUsersJson));
    }

    private OrganizationResponse createOrganization(IdentityUser user, OrganizationRequest organizationRequest) {
        Organization organization = conversionService.convert(organizationRequest, Organization.class);
        organization = organizationService.create(user, organization);
        notify(user, ResourceEvent.ORGANIZATION_CREATED);
        return conversionService.convert(organization, OrganizationResponse.class);
    }

    private Map<String, Set<String>> jsonToMap(Set<ChangeOrganizationUsersJson> changeOrganizationUsersJsons) {
        return changeOrganizationUsersJsons.stream()
                .collect(Collectors.toMap(ChangeOrganizationUsersJson::getUserName, ChangeOrganizationUsersJson::getPermissions));
    }

    private Set<OrganizationResponse> toJsonSet(Set<Organization> organizations) {
        return (Set<OrganizationResponse>) conversionService.convert(organizations, TypeDescriptor.forObject(organizations),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(OrganizationResponse.class)));
    }

}

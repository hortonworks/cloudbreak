package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintLoaderService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Transactional(TxType.NEVER)
public class BlueprintController extends NotificationController implements BlueprintEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintController.class);

    @Autowired
    private BlueprintService blueprintService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private BlueprintLoaderService blueprintLoaderService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public BlueprintResponse get(Long id) {
        return conversionService.convert(blueprintService.get(id), BlueprintResponse.class);
    }

    @Override
    public void delete(Long id) {
        Blueprint deleted = blueprintService.delete(id);
        notify(ResourceEvent.RECIPE_DELETED);
        conversionService.convert(deleted, BlueprintResponse.class);
    }

    @Override
    public BlueprintResponse postPublic(BlueprintRequest request) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return createInOrganization(request, user, organization);
    }

    @Override
    public BlueprintResponse postPrivate(BlueprintRequest request) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return createInOrganization(request, user, organization);
    }

    @Override
    public Set<BlueprintResponse> getPrivates() {
        return listForUsersDefaultOrganization();
    }

    @Override
    public Set<BlueprintResponse> getPublics() {
        return listForUsersDefaultOrganization();
    }

    @Override
    public BlueprintResponse getPrivate(String name) {
        return getBlueprintResponse(name);
    }

    @Override
    public BlueprintResponse getPublic(String name) {
        return getBlueprintResponse(name);
    }

    @Override
    public void deletePublic(String name) {
        deleteInDefaultOrganization(name);
    }

    @Override
    public BlueprintRequest getRequestfromId(Long id) {
        Blueprint blueprint = blueprintService.get(id);
        return conversionService.convert(blueprint, BlueprintRequest.class);
    }

    @Override
    public void deletePrivate(String name) {
        deleteInDefaultOrganization(name);
    }

    private BlueprintResponse getBlueprintResponse(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return conversionService.convert(blueprintService.getByNameForOrganization(name, organization), BlueprintResponse.class);
    }

    private Set<BlueprintResponse> listForUsersDefaultOrganization() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return getBlueprintResponses(user, blueprintService.getAllAvailableInOrganization(organization.getId()), organization);
    }

    private Set<BlueprintResponse> getBlueprintResponses(User user, Set<Blueprint> blueprints, Organization organization) {
        if (blueprintLoaderService.addingDefaultBlueprintsAreNecessaryForTheUser(blueprints)) {
            LOGGER.info("Blueprints should modify based on the defaults for the '{}' user.", user.getUserId());
            blueprints = blueprintLoaderService.loadBlueprintsForTheSpecifiedUser(user, blueprints, organization);
            LOGGER.info("Blueprints modification finished based on the defaults for '{}' user.", user.getUserId());
        }
        return toJsonList(blueprints);
    }

    private Set<BlueprintResponse> toJsonList(Set<Blueprint> blueprints) {
        return (Set<BlueprintResponse>) conversionService.convert(blueprints,
                TypeDescriptor.forObject(blueprints),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(BlueprintResponse.class)));
    }

    private void deleteInDefaultOrganization(String name) {
        executeAndNotify(identityUser -> blueprintService.deleteByNameFromOrganization(name, restRequestThreadLocalService.getRequestedOrgId()),
                ResourceEvent.BLUEPRINT_DELETED);
    }

    private BlueprintResponse createInOrganization(BlueprintRequest request, User user, Organization organization) {
        Blueprint blueprint = conversionService.convert(request, Blueprint.class);
        blueprint = blueprintService.create(blueprint, organization, user);
        return notifyAndReturn(blueprint, ResourceEvent.BLUEPRINT_CREATED);
    }

    private BlueprintResponse notifyAndReturn(Blueprint blueprint, ResourceEvent resourceEvent) {
        notify(resourceEvent);
        return conversionService.convert(blueprint, BlueprintResponse.class);
    }
}

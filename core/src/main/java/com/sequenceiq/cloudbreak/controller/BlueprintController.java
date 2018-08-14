package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

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
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintLoaderService;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@Component
@Transactional(TxType.NEVER)
public class BlueprintController extends NotificationController implements BlueprintEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintController.class);

    @Autowired
    private BlueprintService blueprintService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private BlueprintLoaderService blueprintLoaderService;

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
        return createInDefaultOrganization(request);
    }

    @Override
    public BlueprintResponse postPrivate(BlueprintRequest request) {
        return createInDefaultOrganization(request);
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
        return conversionService.convert(blueprintService.getByNameFromUsersDefaultOrganization(name), BlueprintResponse.class);
    }

    private Set<BlueprintResponse> listForUsersDefaultOrganization() {
        IdentityUser user = authenticatedUserService.getCbUser();
        return getBlueprintResponses(user, blueprintService.findAllForUsersDefaultOrganization());
    }

    private Set<BlueprintResponse> getBlueprintResponses(IdentityUser user, Set<Blueprint> blueprints) {
        if (blueprintLoaderService.addingDefaultBlueprintsAreNecessaryForTheUser(blueprints)) {
            LOGGER.info("Blueprints should modify based on the defaults for the '{}' user.", user.getUserId());
            blueprints = blueprintLoaderService.loadBlueprintsForTheSpecifiedUser(user, blueprints);
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
        executeAndNotify(user -> blueprintService.deleteByNameFromDefaultOrganization(name), ResourceEvent.BLUEPRINT_DELETED);
    }

    private BlueprintResponse createInDefaultOrganization(BlueprintRequest request) {
        Blueprint blueprint = conversionService.convert(request, Blueprint.class);
        blueprint = blueprintService.createInDefaultOrganization(blueprint);
        return notifyAndReturn(blueprint, ResourceEvent.BLUEPRINT_CREATED);
    }

    private BlueprintResponse notifyAndReturn(Blueprint blueprint, ResourceEvent resourceEvent) {
        notify(resourceEvent);
        return conversionService.convert(blueprint, BlueprintResponse.class);
    }
}

package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintLoaderService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@Component
public class BlueprintController implements BlueprintEndpoint {

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
    public IdJson postPrivate(BlueprintRequest blueprintRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return createBlueprint(user, blueprintRequest, false);
    }

    @Override
    public IdJson postPublic(BlueprintRequest blueprintRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return createBlueprint(user, blueprintRequest, true);
    }

    @Override
    public Set<BlueprintResponse> getPrivates() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Set<Blueprint> blueprints = blueprintService.retrievePrivateBlueprints(user);
        if (blueprints.isEmpty()) {
            blueprints = blueprintLoaderService.loadBlueprints(user);
        }
        return toJsonList(blueprints);
    }

    @Override
    public BlueprintResponse getPrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Blueprint blueprint = blueprintService.getPrivateBlueprint(name, user);
        return conversionService.convert(blueprint, BlueprintResponse.class);
    }

    @Override
    public BlueprintResponse getPublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        Blueprint blueprint = blueprintService.getPublicBlueprint(name, user);
        return conversionService.convert(blueprint, BlueprintResponse.class);
    }

    @Override
    public Set<BlueprintResponse> getPublics() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        blueprintLoaderService.loadBlueprints(user);
        Set<Blueprint> blueprints = blueprintService.retrieveAccountBlueprints(user);
        return toJsonList(blueprints);
    }

    @Override
    public BlueprintResponse get(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Blueprint blueprint = blueprintService.get(id);
        return conversionService.convert(blueprint, BlueprintResponse.class);
    }

    @Override
    public void delete(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        blueprintService.delete(id, user);
    }

    @Override
    public void deletePublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        blueprintService.delete(name, user);
    }

    @Override
    public void deletePrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        blueprintService.delete(name, user);
    }

    private IdJson createBlueprint(CbUser user, BlueprintRequest blueprintRequest, boolean publicInAccount) {
        Blueprint blueprint = conversionService.convert(blueprintRequest, Blueprint.class);
        blueprint.setPublicInAccount(publicInAccount);
        blueprint = blueprintService.create(user, blueprint, blueprintRequest.getProperties());
        return new IdJson(blueprint.getId());
    }

    private Set<BlueprintResponse> toJsonList(Set<Blueprint> blueprints) {
        return (Set<BlueprintResponse>) conversionService.convert(blueprints,
                TypeDescriptor.forObject(blueprints),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(BlueprintResponse.class)));
    }

}

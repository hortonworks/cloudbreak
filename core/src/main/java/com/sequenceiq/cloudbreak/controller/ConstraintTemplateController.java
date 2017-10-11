package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import com.sequenceiq.cloudbreak.api.endpoint.v1.ConstraintTemplateEndpoint;
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;
import com.sequenceiq.cloudbreak.service.constraint.ConstraintTemplateService;

@Component
public class ConstraintTemplateController implements ConstraintTemplateEndpoint {
    @Autowired
    private ConstraintTemplateService constraintTemplateService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public ConstraintTemplateResponse postPrivate(ConstraintTemplateRequest constraintTemplateRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createConstraintTemplate(user, constraintTemplateRequest, false);
    }

    @Override
    public ConstraintTemplateResponse postPublic(ConstraintTemplateRequest constraintTemplateRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createConstraintTemplate(user, constraintTemplateRequest, true);
    }

    @Override
    public Set<ConstraintTemplateResponse> getPrivates() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<ConstraintTemplate> constraintTemplates = constraintTemplateService.retrievePrivateConstraintTemplates(user);
        return convert(constraintTemplates);
    }

    @Override
    public Set<ConstraintTemplateResponse> getPublics() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<ConstraintTemplate> templates = constraintTemplateService.retrieveAccountConstraintTemplates(user);
        return convert(templates);
    }

    @Override
    public ConstraintTemplateResponse get(Long id) {
        ConstraintTemplate template = constraintTemplateService.get(id);
        return convert(template);
    }

    @Override
    public ConstraintTemplateResponse getPrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        ConstraintTemplate template = constraintTemplateService.getPrivateTemplate(name, user);
        return convert(template);
    }

    @Override
    public ConstraintTemplateResponse getPublic(@PathVariable String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        ConstraintTemplate template = constraintTemplateService.getPublicTemplate(name, user);
        return convert(template);
    }

    @Override
    public void delete(Long id) {
        IdentityUser user = authenticatedUserService.getCbUser();
        constraintTemplateService.delete(id, user);
    }

    @Override
    public void deletePublic(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        constraintTemplateService.delete(name, user);
    }

    @Override
    public void deletePrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        constraintTemplateService.delete(name, user);
    }

    private ConstraintTemplateResponse createConstraintTemplate(IdentityUser user, ConstraintTemplateRequest constraintTemplateRequest,
            boolean publicInAccount) {
        ConstraintTemplate constraintTemplate = convert(constraintTemplateRequest, publicInAccount);
        constraintTemplate = constraintTemplateService.create(user, constraintTemplate);
        return conversionService.convert(constraintTemplate, ConstraintTemplateResponse.class);
    }

    private ConstraintTemplate convert(ConstraintTemplateRequest constraintTemplateRequest, boolean publicInAccount) {
        ConstraintTemplate converted = conversionService.convert(constraintTemplateRequest, ConstraintTemplate.class);
        converted.setPublicInAccount(publicInAccount);
        return converted;
    }

    private ConstraintTemplateResponse convert(ConstraintTemplate constraintTemplate) {
        return conversionService.convert(constraintTemplate, ConstraintTemplateResponse.class);
    }

    private Set<ConstraintTemplateResponse> convert(Set<ConstraintTemplate> constraintTemplates) {
        Set<ConstraintTemplateResponse> jsons = new HashSet<>();
        for (ConstraintTemplate constraintTemplate : constraintTemplates) {
            jsons.add(convert(constraintTemplate));
        }
        return jsons;
    }

}

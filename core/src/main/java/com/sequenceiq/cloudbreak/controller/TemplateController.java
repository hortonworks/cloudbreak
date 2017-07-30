package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import com.sequenceiq.cloudbreak.api.endpoint.TemplateEndpoint;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidator;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.decorator.Decorator;
import com.sequenceiq.cloudbreak.service.template.DefaultTemplateLoaderService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Component
public class TemplateController implements TemplateEndpoint {
    @Autowired
    private TemplateService templateService;

    @Autowired
    private DefaultTemplateLoaderService defaultTemplateLoaderService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private TemplateValidator templateValidator;

    @Autowired
    private Decorator<Template> templateDecorator;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public TemplateResponse postPrivate(TemplateRequest templateRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createTemplate(user, templateRequest, false);
    }

    @Override
    public TemplateResponse postPublic(TemplateRequest templateRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createTemplate(user, templateRequest, true);
    }

    @Override
    public Set<TemplateResponse> getPrivates() {
        IdentityUser user = authenticatedUserService.getCbUser();
        defaultTemplateLoaderService.createDefaultTemplates(user);
        Set<Template> templates = templateService.retrievePrivateTemplates(user);
        return convert(templates);
    }

    @Override
    public Set<TemplateResponse> getPublics() {
        IdentityUser user = authenticatedUserService.getCbUser();
        defaultTemplateLoaderService.createDefaultTemplates(user);
        Set<Template> templates = templateService.retrieveAccountTemplates(user);
        return convert(templates);
    }

    @Override
    public TemplateResponse get(Long id) {
        Template template = templateService.get(id);
        return convert(template);
    }

    @Override
    public TemplateResponse getPrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Template template = templateService.getPrivateTemplate(name, user);
        return convert(template);
    }

    @Override
    public TemplateResponse getPublic(@PathVariable String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Template template = templateService.getPublicTemplate(name, user);
        return convert(template);
    }

    @Override
    public void delete(Long id) {
        IdentityUser user = authenticatedUserService.getCbUser();
        templateService.delete(id, user);
    }

    @Override
    public void deletePublic(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        templateService.delete(name, user);
    }

    @Override
    public void deletePrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        templateService.delete(name, user);
    }

    private TemplateResponse createTemplate(IdentityUser user, TemplateRequest templateRequest, boolean publicInAccount) {
        Template template = convert(templateRequest, publicInAccount);
        templateValidator.validateTemplateRequest(template);
        template = templateDecorator.decorate(template);
        template = templateService.create(user, template);
        return conversionService.convert(template, TemplateResponse.class);
    }

    private Template convert(TemplateRequest templateRequest, boolean publicInAccount) {
        Template converted = conversionService.convert(templateRequest, Template.class);
        converted.setPublicInAccount(publicInAccount);
        return converted;
    }

    private TemplateResponse convert(Template template) {
        return conversionService.convert(template, TemplateResponse.class);
    }

    private Set<TemplateResponse> convert(Set<Template> templates) {
        Set<TemplateResponse> jsons = new HashSet<>();
        for (Template template : templates) {
            jsons.add(convert(template));
        }
        return jsons;
    }

}

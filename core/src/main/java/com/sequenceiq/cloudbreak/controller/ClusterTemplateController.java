package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.ClusterTemplateEndpoint;
import com.sequenceiq.cloudbreak.api.model.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.ClusterTemplate;
import com.sequenceiq.cloudbreak.service.clustertemplate.ClusterTemplateService;

@Component
public class ClusterTemplateController implements ClusterTemplateEndpoint {

    @Autowired
    private ClusterTemplateService clusterTemplateService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public ClusterTemplateResponse postPrivate(ClusterTemplateRequest clusterTemplateRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createClusterTemplateRequest(user, clusterTemplateRequest, false);
    }

    @Override
    public ClusterTemplateResponse postPublic(ClusterTemplateRequest clusterTemplateRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createClusterTemplateRequest(user, clusterTemplateRequest, true);
    }

    @Override
    public Set<ClusterTemplateResponse> getPrivates() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<ClusterTemplate> clusterTemplates = clusterTemplateService.retrievePrivateClusterTemplates(user);
        return toJsonList(clusterTemplates);
    }

    @Override
    public ClusterTemplateResponse getPrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        ClusterTemplate clusterTemplate = clusterTemplateService.getPrivateClusterTemplate(name, user);
        return conversionService.convert(clusterTemplate, ClusterTemplateResponse.class);
    }

    @Override
    public ClusterTemplateResponse getPublic(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        ClusterTemplate clusterTemplate = clusterTemplateService.getPublicClusterTemplate(name, user);
        return conversionService.convert(clusterTemplate, ClusterTemplateResponse.class);
    }

    @Override
    public Set<ClusterTemplateResponse> getPublics() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<ClusterTemplate> clusterTemplates = clusterTemplateService.retrieveAccountClusterTemplates(user);
        return toJsonList(clusterTemplates);
    }

    @Override
    public ClusterTemplateResponse get(Long id) {
        IdentityUser user = authenticatedUserService.getCbUser();
        ClusterTemplate clusterTemplate = clusterTemplateService.get(id);
        return conversionService.convert(clusterTemplate, ClusterTemplateResponse.class);
    }

    @Override
    public void delete(Long id) {
        IdentityUser user = authenticatedUserService.getCbUser();
        clusterTemplateService.delete(id, user);
    }

    @Override
    public void deletePublic(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        clusterTemplateService.delete(name, user);
    }

    @Override
    public void deletePrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        clusterTemplateService.delete(name, user);
    }

    private ClusterTemplateResponse createClusterTemplateRequest(IdentityUser user, ClusterTemplateRequest clusterTemplateRequest, boolean publicInAccount) {
        ClusterTemplate clusterTemplate = conversionService.convert(clusterTemplateRequest, ClusterTemplate.class);
        clusterTemplate.setPublicInAccount(publicInAccount);
        clusterTemplate = clusterTemplateService.create(user, clusterTemplate);
        return conversionService.convert(clusterTemplate, ClusterTemplateResponse.class);
    }

    private Set<ClusterTemplateResponse> toJsonList(Set<ClusterTemplate> clusterTemplates) {
        return (Set<ClusterTemplateResponse>) conversionService.convert(clusterTemplates,
                TypeDescriptor.forObject(clusterTemplates),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(ClusterTemplateResponse.class)));
    }
}

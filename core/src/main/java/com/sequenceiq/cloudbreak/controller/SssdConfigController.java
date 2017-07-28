package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.SssdConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest;
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.SssdConfig;
import com.sequenceiq.cloudbreak.service.sssdconfig.SssdConfigService;

@Component
public class SssdConfigController implements SssdConfigEndpoint {

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private SssdConfigService sssdConfigService;

    @Override
    public SssdConfigResponse postPrivate(SssdConfigRequest sssdConfigRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createConfig(user, sssdConfigRequest, false);
    }

    @Override
    public SssdConfigResponse postPublic(SssdConfigRequest sssdConfigRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createConfig(user, sssdConfigRequest, true);
    }

    @Override
    public Set<SssdConfigResponse> getPrivates() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<SssdConfig> configs = sssdConfigService.retrievePrivateConfigs(user);
        return toJsonSet(configs);
    }

    @Override
    public Set<SssdConfigResponse> getPublics() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<SssdConfig> configs = sssdConfigService.retrieveAccountConfigs(user);
        return toJsonSet(configs);
    }

    @Override
    public SssdConfigResponse getPrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        SssdConfig config = sssdConfigService.getPrivateConfig(name, user);
        return conversionService.convert(config, SssdConfigResponse.class);
    }

    @Override
    public SssdConfigResponse getPublic(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        SssdConfig config = sssdConfigService.getPublicConfig(name, user);
        return conversionService.convert(config, SssdConfigResponse.class);
    }

    @Override
    public SssdConfigResponse get(Long id) {
        SssdConfig config = sssdConfigService.get(id);
        return conversionService.convert(config, SssdConfigResponse.class);
    }

    @Override
    public void delete(Long id) {
        sssdConfigService.delete(id);
    }

    @Override
    public void deletePublic(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        sssdConfigService.delete(name, user);
    }

    @Override
    public void deletePrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        sssdConfigService.delete(name, user);
    }

    private SssdConfigResponse createConfig(IdentityUser user, SssdConfigRequest request, boolean publicInAccount) {
        SssdConfig config = conversionService.convert(request, SssdConfig.class);
        config.setPublicInAccount(publicInAccount);
        config = sssdConfigService.create(user, config);
        return conversionService.convert(config, SssdConfigResponse.class);
    }

    private Set<SssdConfigResponse> toJsonSet(Set<SssdConfig> configs) {
        return (Set<SssdConfigResponse>) conversionService.convert(configs, TypeDescriptor.forObject(configs),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(SssdConfigResponse.class)));
    }
}

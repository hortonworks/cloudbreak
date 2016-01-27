package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.SssdConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest;
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.SssdConfig;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
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
    public IdJson postPrivate(SssdConfigRequest sssdConfigRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return createConfig(user, sssdConfigRequest, false);
    }

    @Override
    public IdJson postPublic(SssdConfigRequest sssdConfigRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return createConfig(user, sssdConfigRequest, true);
    }

    @Override
    public Set<SssdConfigResponse> getPrivates() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Set<SssdConfig> configs = sssdConfigService.retrievePrivateConfigs(user);
        return toJsonSet(configs);
    }

    @Override
    public Set<SssdConfigResponse> getPublics() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Set<SssdConfig> configs = sssdConfigService.retrieveAccountConfigs(user);
        return toJsonSet(configs);
    }

    @Override
    public SssdConfigResponse getPrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        SssdConfig config = sssdConfigService.getPrivateConfig(name, user);
        return conversionService.convert(config, SssdConfigResponse.class);
    }

    @Override
    public SssdConfigResponse getPublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        SssdConfig config = sssdConfigService.getPublicConfig(name, user);
        return conversionService.convert(config, SssdConfigResponse.class);
    }

    @Override
    public SssdConfigResponse get(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        SssdConfig config = sssdConfigService.get(id);
        return conversionService.convert(config, SssdConfigResponse.class);
    }

    @Override
    public void delete(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        sssdConfigService.delete(id, user);
    }

    @Override
    public void deletePublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        sssdConfigService.delete(name, user);
    }

    @Override
    public void deletePrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        sssdConfigService.delete(name, user);
    }

    private IdJson createConfig(CbUser user, SssdConfigRequest request, boolean publicInAccount) {
        SssdConfig config = conversionService.convert(request, SssdConfig.class);
        config.setPublicInAccount(publicInAccount);
        config = sssdConfigService.create(user, config);
        return new IdJson(config.getId());
    }

    private Set<SssdConfigResponse> toJsonSet(Set<SssdConfig> configs) {
        return (Set<SssdConfigResponse>) conversionService.convert(configs, TypeDescriptor.forObject(configs),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(SssdConfigResponse.class)));
    }
}

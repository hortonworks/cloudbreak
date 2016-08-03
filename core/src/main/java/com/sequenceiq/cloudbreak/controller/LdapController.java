package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import com.sequenceiq.cloudbreak.api.endpoint.LdapConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.LdapConfigResponse;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;

public class LdapController implements LdapConfigEndpoint {

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private LdapConfigService ldapConfigService;

    @Override
    public IdJson postPrivate(LdapConfigRequest ldapConfigRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return createConfig(user, ldapConfigRequest, false);
    }

    @Override
    public IdJson postPublic(LdapConfigRequest ldapConfigRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return createConfig(user, ldapConfigRequest, true);
    }

    @Override
    public Set<LdapConfigResponse> getPrivates() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Set<LdapConfig> configs = ldapConfigService.retrievePrivateConfigs(user);
        return toJsonSet(configs);
    }

    @Override
    public Set<LdapConfigResponse> getPublics() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Set<LdapConfig> configs = ldapConfigService.retrieveAccountConfigs(user);
        return toJsonSet(configs);
    }

    @Override
    public LdapConfigResponse getPrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        LdapConfig config = ldapConfigService.getPrivateConfig(name, user);
        return conversionService.convert(config, LdapConfigResponse.class);
    }

    @Override
    public LdapConfigResponse getPublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        LdapConfig config = ldapConfigService.getPublicConfig(name, user);
        return conversionService.convert(config, LdapConfigResponse.class);
    }

    @Override
    public LdapConfigResponse get(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        LdapConfig config = ldapConfigService.get(id);
        return conversionService.convert(config, LdapConfigResponse.class);
    }

    @Override
    public void delete(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        ldapConfigService.delete(id, user);
    }

    @Override
    public void deletePublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        ldapConfigService.delete(name, user);
    }

    @Override
    public void deletePrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        ldapConfigService.delete(name, user);
    }

    private IdJson createConfig(CbUser user, LdapConfigRequest request, boolean publicInAccount) {
        LdapConfig config = conversionService.convert(request, LdapConfig.class);
        config.setPublicInAccount(publicInAccount);
        config = ldapConfigService.create(user, config);
        return new IdJson(config.getId());
    }

    private Set<LdapConfigResponse> toJsonSet(Set<LdapConfig> configs) {
        return (Set<LdapConfigResponse>) conversionService.convert(configs, TypeDescriptor.forObject(configs),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(LdapConfigResponse.class)));
    }
}

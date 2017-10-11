package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import com.sequenceiq.cloudbreak.api.endpoint.v1.LdapConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.LdapConfigResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;

public class LdapController extends NotificationController implements LdapConfigEndpoint {

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private LdapConfigService ldapConfigService;

    @Override
    public LdapConfigResponse postPrivate(LdapConfigRequest ldapConfigRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createConfig(user, ldapConfigRequest, false);
    }

    @Override
    public LdapConfigResponse postPublic(LdapConfigRequest ldapConfigRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createConfig(user, ldapConfigRequest, true);
    }

    @Override
    public Set<LdapConfigResponse> getPrivates() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<LdapConfig> configs = ldapConfigService.retrievePrivateConfigs(user);
        return toJsonSet(configs);
    }

    @Override
    public Set<LdapConfigResponse> getPublics() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<LdapConfig> configs = ldapConfigService.retrieveAccountConfigs(user);
        return toJsonSet(configs);
    }

    @Override
    public LdapConfigResponse getPrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        LdapConfig config = ldapConfigService.getPrivateConfig(name, user);
        return conversionService.convert(config, LdapConfigResponse.class);
    }

    @Override
    public LdapConfigResponse getPublic(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        LdapConfig config = ldapConfigService.getPublicConfig(name, user);
        return conversionService.convert(config, LdapConfigResponse.class);
    }

    @Override
    public LdapConfigResponse get(Long id) {
        LdapConfig config = ldapConfigService.get(id);
        return conversionService.convert(config, LdapConfigResponse.class);
    }

    @Override
    public void delete(Long id) {
        executeAndNotify(user -> ldapConfigService.delete(id, user), ResourceEvent.LDAP_DELETED);
    }

    @Override
    public void deletePublic(String name) {
        executeAndNotify(user -> ldapConfigService.delete(name, user), ResourceEvent.LDAP_DELETED);
    }

    @Override
    public void deletePrivate(String name) {
        executeAndNotify(user -> ldapConfigService.delete(name, user), ResourceEvent.LDAP_DELETED);
    }

    private LdapConfigResponse createConfig(IdentityUser user, LdapConfigRequest request, boolean publicInAccount) {
        LdapConfig config = conversionService.convert(request, LdapConfig.class);
        config.setPublicInAccount(publicInAccount);
        config = ldapConfigService.create(user, config);
        notify(user, ResourceEvent.LDAP_CREATED);
        return conversionService.convert(config, LdapConfigResponse.class);
    }

    private Set<LdapConfigResponse> toJsonSet(Set<LdapConfig> configs) {
        return (Set<LdapConfigResponse>) conversionService.convert(configs, TypeDescriptor.forObject(configs),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(LdapConfigResponse.class)));
    }
}

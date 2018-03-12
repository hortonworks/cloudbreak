package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import com.sequenceiq.cloudbreak.api.endpoint.v1.LdapConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.ldap.LDAPTestRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapTestResult;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapValidationRequest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;

public class LdapController extends NotificationController implements LdapConfigEndpoint {

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private LdapConfigValidator ldapConfigValidator;

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

    @Override
    public LdapTestResult testLdapConnection(LDAPTestRequest ldapTestRequest) {
        Long existingLDAPConfigId = ldapTestRequest.getId();
        LdapValidationRequest validationRequest = ldapTestRequest.getValidationRequest();
        if (existingLDAPConfigId == null && validationRequest == null) {
            throw new BadRequestException("Either an existing resource 'id' or an LDAP 'validationRequest' needs to be specified in the request. ");
        }

        LdapTestResult ldapTestResult = new LdapTestResult();
        try {
            if (existingLDAPConfigId != null) {
                LdapConfig ldapConfig = ldapConfigService.get(existingLDAPConfigId);
                ldapConfigValidator.validateLdapConnection(ldapConfig);
            } else {
                ldapConfigValidator.validateLdapConnection(validationRequest);
            }
            ldapTestResult.setConnectionResult("connected");
        } catch (BadRequestException e) {
            ldapTestResult.setConnectionResult(e.getMessage());
        }
        return ldapTestResult;
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

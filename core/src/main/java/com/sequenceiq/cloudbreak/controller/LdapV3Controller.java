package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v3.LdapConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.ldap.LDAPTestRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapTestResult;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapValidationRequest;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;

@Component
@Transactional(TxType.NEVER)
public class LdapV3Controller extends NotificationController implements LdapConfigV3Endpoint {

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private LdapConfigValidator ldapConfigValidator;

    @Override
    public Set<LdapConfigResponse> listConfigsByOrganization(Long organizationId) {
        return ldapConfigService.listByOrganizationId(organizationId).stream()
                .map(ldapConfig -> conversionService.convert(ldapConfig, LdapConfigResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public LdapConfigResponse getByNameInOrganization(Long organizationId, String name) {
        LdapConfig ldapConfig = ldapConfigService.getByNameForOrganizationId(name, organizationId);
        return conversionService.convert(ldapConfig, LdapConfigResponse.class);
    }

    @Override
    public LdapConfigResponse createInOrganization(Long organizationId, LdapConfigRequest request) {
        LdapConfig ldapConfig = conversionService.convert(request, LdapConfig.class);
        ldapConfig = ldapConfigService.create(ldapConfig, organizationId);
        notify(authenticatedUserService.getCbUser(), ResourceEvent.LDAP_CREATED);
        return conversionService.convert(ldapConfig, LdapConfigResponse.class);
    }

    @Override
    public LdapConfigResponse deleteInOrganization(Long organizationId, String name) {
        LdapConfig config = ldapConfigService.deleteByNameFromOrganization(name, organizationId);
        notify(authenticatedUserService.getCbUser(), ResourceEvent.LDAP_DELETED);
        return conversionService.convert(config, LdapConfigResponse.class);
    }

    @Override
    public LdapTestResult testLdapConnection(Long organizationId, LDAPTestRequest ldapValidationRequest) {
        String existingLDAPConfigName = ldapValidationRequest.getName();
        LdapValidationRequest validationRequest = ldapValidationRequest.getValidationRequest();
        if (existingLDAPConfigName == null && validationRequest == null) {
            throw new BadRequestException("Either an existing resource 'name' or an LDAP 'validationRequest' needs to be specified in the request. ");
        }

        LdapTestResult ldapTestResult = new LdapTestResult();
        try {
            if (existingLDAPConfigName != null) {
                LdapConfig ldapConfig = ldapConfigService.getByNameForOrganizationId(existingLDAPConfigName, organizationId);
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
}

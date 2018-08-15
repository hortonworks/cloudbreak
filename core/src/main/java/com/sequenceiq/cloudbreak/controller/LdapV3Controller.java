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
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
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

    @Override
    public Set<LdapConfigResponse> listConfigsByOrganization(Long organizationId) {
        return ldapConfigService.listByOrganizationId(organizationId).stream()
                .map(ldapConfig -> conversionService.convert(ldapConfig, LdapConfigResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public LdapConfigResponse getByNameInOrganization(Long organizationId, String ldapConfigName) {
        LdapConfig ldapConfig = ldapConfigService.getByNameForOrganization(ldapConfigName, organizationId);
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
    public LdapConfigResponse deleteInOrganization(Long organizationId, String configName) {
        LdapConfig config = ldapConfigService.deleteByNameFromOrganization(configName, organizationId);
        notify(authenticatedUserService.getCbUser(), ResourceEvent.LDAP_DELETED);
        return conversionService.convert(config, LdapConfigResponse.class);
    }
}

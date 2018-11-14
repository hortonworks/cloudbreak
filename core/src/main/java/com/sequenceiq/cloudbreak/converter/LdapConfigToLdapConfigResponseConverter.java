package com.sequenceiq.cloudbreak.converter;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class LdapConfigToLdapConfigResponseConverter extends AbstractConversionServiceAwareConverter<LdapConfig, LdapConfigResponse> {

    @Inject
    private ConversionService conversionService;

    @Override
    public LdapConfigResponse convert(LdapConfig config) {
        LdapConfigResponse json = new LdapConfigResponse();
        json.setName(config.getName());
        json.setDescription(config.getDescription());
        json.setId(config.getId());
        json.setServerHost(config.getServerHost());
        json.setServerPort(config.getServerPort());
        json.setProtocol(config.getProtocol());
        json.setBindDn(conversionService.convert(config.getBindDnSecret(), SecretResponse.class));
        json.setBindPassword(conversionService.convert(config.getBindPasswordSecret(), SecretResponse.class));
        json.setGroupSearchBase(config.getGroupSearchBase());
        json.setUserSearchBase(config.getUserSearchBase());
        json.setUserDnPattern(config.getUserDnPattern());
        json.setUserNameAttribute(config.getUserNameAttribute());
        json.setDomain(config.getDomain());
        json.setDirectoryType(config.getDirectoryType());
        json.setUserObjectClass(config.getUserObjectClass());
        json.setGroupObjectClass(config.getGroupObjectClass());
        json.setGroupNameAttribute(config.getGroupNameAttribute());
        json.setGroupMemberAttribute(config.getGroupMemberAttribute());
        json.setAdminGroup(config.getAdminGroup());
        json.setCertificate(config.getCertificate());
        json.setEnvironments(config.getEnvironments().stream().map(CompactView::getName).collect(Collectors.toSet()));
        return json;
    }
}

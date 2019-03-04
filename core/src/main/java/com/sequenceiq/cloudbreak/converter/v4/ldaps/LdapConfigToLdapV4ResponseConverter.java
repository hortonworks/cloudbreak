package com.sequenceiq.cloudbreak.converter.v4.ldaps;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class LdapConfigToLdapV4ResponseConverter extends AbstractConversionServiceAwareConverter<LdapConfig, LdapV4Response> {

    @Override
    public LdapV4Response convert(LdapConfig config) {
        LdapV4Response json = new LdapV4Response();
        json.setName(config.getName());
        json.setDescription(config.getDescription());
        json.setId(config.getId());
        json.setHost(config.getServerHost());
        json.setPort(config.getServerPort());
        json.setProtocol(config.getProtocol());
        json.setBindDn(getConversionService().convert(config.getBindDnSecret(), SecretV4Response.class));
        json.setBindPassword(getConversionService().convert(config.getBindPasswordSecret(), SecretV4Response.class));
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

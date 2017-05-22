package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.LdapConfigResponse;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

@Component
public class LdapConfigToJsonConverter extends AbstractConversionServiceAwareConverter<LdapConfig, LdapConfigResponse> {
    @Override
    public LdapConfigResponse convert(LdapConfig config) {
        LdapConfigResponse json = new LdapConfigResponse();
        json.setName(config.getName());
        json.setDescription(config.getDescription());
        json.setId(config.getId());
        json.setPublicInAccount(config.isPublicInAccount());
        json.setBindDn(config.getBindDn());
        json.setServerHost(config.getServerHost());
        json.setServerPort(config.getServerPort());
        json.setProtocol(config.getProtocol());
        json.setGroupSearchBase(config.getGroupSearchBase());
        json.setGroupSearchFilter(config.getGroupSearchFilter());
        json.setUserSearchBase(config.getUserSearchBase());
        json.setUserSearchFilter(config.getUserSearchFilter());
        json.setPrincipalRegex(config.getPrincipalRegex());
        json.setUserSearchAttribute(config.getUserSearchAttribute());
        json.setDomain(config.getDomain());
        json.setDirectoryType(config.getDirectoryType());
        json.setUserObjectClass(config.getUserObjectClass());
        json.setGroupObjectClass(config.getGroupObjectClass());
        json.setGroupIdAttribute(config.getGroupIdAttribute());
        json.setGroupMemberAttribute(config.getGroupMemberAttribute());

        return json;
    }
}

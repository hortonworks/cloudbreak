package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

@Component
public class LdapConfigToLdapConfigResponseConverter extends AbstractConversionServiceAwareConverter<LdapConfig, LdapConfigResponse> {
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
        return json;
    }
}

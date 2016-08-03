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
        json.setBindPassword(config.getBindPassword());
        json.setServerHost(config.getServerHost());
        json.setServerPort(config.getServerPort());
        json.setServerSSL(config.getServerSSL());
        json.setGroupSearchBase(config.getGroupSearchBase());
        json.setGroupSearchFilter(config.getGroupSearchFilter());
        json.setUserSearchBase(config.getUserSearchBase());
        json.setUserSearchFilter(config.getUserSearchFilter());
        json.setPrincipalRegex(config.getPrincipalRegex());

        return json;
    }
}

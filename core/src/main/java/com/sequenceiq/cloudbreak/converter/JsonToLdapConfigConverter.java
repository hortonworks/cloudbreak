package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.LdapConfigRequest;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

@Component
public class JsonToLdapConfigConverter extends AbstractConversionServiceAwareConverter<LdapConfigRequest, LdapConfig> {

    @Override
    public LdapConfig convert(LdapConfigRequest json) {
        LdapConfig config = new LdapConfig();
        config.setName(json.getName());
        config.setDescription(json.getDescription());
        config.setBindDn(json.getBindDn());
        config.setBindPassword(json.getBindPassword());
        config.setServerHost(json.getServerHost());
        config.setServerPort(json.getServerPort());
        config.setProtocol(json.getProtocol());
        config.setGroupSearchBase(json.getGroupSearchBase());
        config.setGroupSearchFilter(json.getGroupSearchFilter());
        config.setUserSearchBase(json.getUserSearchBase());
        if (json.getUserSearchFilter() == null) {
            config.setUserSearchFilter("cn");
        } else {
            config.setUserSearchFilter(json.getUserSearchFilter());
        }
        config.setPrincipalRegex(json.getPrincipalRegex());
        config.setUserSearchAttribute(json.getUserSearchAttribute());
        config.setDomain(json.getDomain());
        return config;
    }
}

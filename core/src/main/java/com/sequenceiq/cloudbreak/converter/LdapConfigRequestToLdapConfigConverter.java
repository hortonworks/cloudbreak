package com.sequenceiq.cloudbreak.converter;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.api.model.DirectoryType;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class LdapConfigRequestToLdapConfigConverter extends AbstractConversionServiceAwareConverter<LdapConfigRequest, LdapConfig> {

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public LdapConfig convert(LdapConfigRequest json) {
        LdapConfig config = new LdapConfig();
        if (Strings.isNullOrEmpty(json.getName())) {
            config.setName(missingResourceNameGenerator.generateName(APIResourceType.LDAP_CONFIG));
        } else {
            config.setName(json.getName());
        }
        config.setDescription(json.getDescription());
        config.setBindDn(json.getBindDn());
        config.setBindPassword(json.getBindPassword());
        config.setServerHost(json.getServerHost());
        config.setServerPort(json.getServerPort());
        config.setProtocol(json.getProtocol());
        config.setGroupSearchBase(json.getGroupSearchBase());
        config.setUserSearchBase(json.getUserSearchBase());
        config.setUserDnPattern(json.getUserDnPattern());
        config.setUserNameAttribute(json.getUserNameAttribute());
        config.setDomain(Optional.ofNullable(json.getDomain()).orElse(""));
        config.setDirectoryType(json.getDirectoryType() != null ? json.getDirectoryType() : DirectoryType.LDAP);
        config.setUserObjectClass(json.getUserObjectClass() != null ? json.getUserObjectClass() : "person");
        config.setGroupObjectClass(json.getGroupObjectClass() != null ? json.getGroupObjectClass() : "groupOfNames");
        config.setGroupNameAttribute(json.getGroupNameAttribute() != null ? json.getGroupNameAttribute() : "cn");
        config.setGroupMemberAttribute(json.getGroupMemberAttribute() != null ? json.getGroupMemberAttribute() : "member");
        config.setAdminGroup(Optional.ofNullable(json.getAdminGroup()).orElse(""));
        return config;
    }
}

package com.sequenceiq.cloudbreak.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

@Component
public class LdapConfigToLdapConfigRequestConverter extends AbstractConversionServiceAwareConverter<LdapConfig, LdapConfigRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigToLdapConfigRequestConverter.class);

    @Override
    public LdapConfigRequest convert(LdapConfig source) {
        LdapConfigRequest ldapConfigRequest = new LdapConfigRequest();
        ldapConfigRequest.setName(source.getName());
        ldapConfigRequest.setBindDn("fake-user");
        ldapConfigRequest.setBindPassword("fake-password");
        ldapConfigRequest.setAdminGroup(source.getAdminGroup());
        ldapConfigRequest.setDescription(source.getDescription());
        ldapConfigRequest.setDirectoryType(source.getDirectoryType());
        ldapConfigRequest.setDomain(source.getDomain());
        ldapConfigRequest.setGroupMemberAttribute(source.getGroupMemberAttribute());
        ldapConfigRequest.setGroupNameAttribute(source.getGroupNameAttribute());
        ldapConfigRequest.setGroupObjectClass(source.getGroupObjectClass());
        ldapConfigRequest.setGroupSearchBase(source.getGroupSearchBase());
        ldapConfigRequest.setProtocol(source.getProtocol());
        ldapConfigRequest.setServerHost(source.getServerHost());
        ldapConfigRequest.setServerPort(source.getServerPort());
        ldapConfigRequest.setUserDnPattern(source.getUserDnPattern());
        ldapConfigRequest.setUserNameAttribute(source.getUserNameAttribute());
        ldapConfigRequest.setUserObjectClass(source.getUserObjectClass());
        ldapConfigRequest.setUserSearchBase(source.getUserSearchBase());
        return ldapConfigRequest;
    }
}
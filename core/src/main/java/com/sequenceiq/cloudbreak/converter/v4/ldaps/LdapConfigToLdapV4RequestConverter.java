package com.sequenceiq.cloudbreak.converter.v4.ldaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

@Component
public class LdapConfigToLdapV4RequestConverter extends AbstractConversionServiceAwareConverter<LdapConfig, LdapV4Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigToLdapV4RequestConverter.class);

    @Override
    public LdapV4Request convert(LdapConfig source) {
        LdapV4Request ldapV4Request = new LdapV4Request();
        ldapV4Request.setName(source.getName());
        ldapV4Request.setBindDn("fake-user");
        ldapV4Request.setBindPassword("fake-password");
        ldapV4Request.setAdminGroup(source.getAdminGroup());
        ldapV4Request.setDescription(source.getDescription());
        ldapV4Request.setDirectoryType(source.getDirectoryType());
        ldapV4Request.setDomain(source.getDomain());
        ldapV4Request.setGroupMemberAttribute(source.getGroupMemberAttribute());
        ldapV4Request.setGroupNameAttribute(source.getGroupNameAttribute());
        ldapV4Request.setGroupObjectClass(source.getGroupObjectClass());
        ldapV4Request.setGroupSearchBase(source.getGroupSearchBase());
        ldapV4Request.setProtocol(source.getProtocol());
        ldapV4Request.setHost(source.getServerHost());
        ldapV4Request.setPort(source.getServerPort());
        ldapV4Request.setUserDnPattern(source.getUserDnPattern());
        ldapV4Request.setUserNameAttribute(source.getUserNameAttribute());
        ldapV4Request.setUserObjectClass(source.getUserObjectClass());
        ldapV4Request.setUserSearchBase(source.getUserSearchBase());
        ldapV4Request.setCertificate(source.getCertificate());
        return ldapV4Request;
    }
}
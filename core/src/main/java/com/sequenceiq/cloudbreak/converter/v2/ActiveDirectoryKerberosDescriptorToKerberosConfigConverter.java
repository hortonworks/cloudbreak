package com.sequenceiq.cloudbreak.converter.v2;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class ActiveDirectoryKerberosDescriptorToKerberosConfigConverter extends
        AbstractConversionServiceAwareConverter<ActiveDirectoryKerberosDescriptor, KerberosConfig> {

    @Override
    public KerberosConfig convert(ActiveDirectoryKerberosDescriptor source) {
        KerberosConfig config = new KerberosConfig();
        config.setAdminUrl(source.getAdminUrl());
        config.setContainerDn(source.getContainerDn());
        config.setLdapUrl(source.getLdapUrl());
        config.setRealm(source.getRealm());
        config.setUrl(source.getUrl());
        config.setPrincipal(source.getPrincipal());
        config.setType(source.getType());
        config.setNameServers(source.getNameServers());
        config.setPassword(source.getPassword());
        config.setVerifyKdcTrust(source.getVerifyKdcTrust());
        config.setTcpAllowed(source.getTcpAllowed());
        if (StringUtils.isNotBlank(source.getDomain())) {
            config.setDomain(source.getDomain());
        } else if (StringUtils.isNotBlank(source.getRealm())) {
            config.setDomain(source.getRealm().toLowerCase());
        }
        return config;
    }

}

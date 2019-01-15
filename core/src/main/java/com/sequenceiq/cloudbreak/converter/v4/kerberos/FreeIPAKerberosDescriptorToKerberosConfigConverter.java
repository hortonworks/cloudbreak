package com.sequenceiq.cloudbreak.converter.v4.kerberos;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class FreeIPAKerberosDescriptorToKerberosConfigConverter extends AbstractConversionServiceAwareConverter<FreeIPAKerberosDescriptor, KerberosConfig> {

    @Override
    public KerberosConfig convert(FreeIPAKerberosDescriptor source) {
        KerberosConfig config = new KerberosConfig();
        config.setAdminUrl(source.getAdminUrl());
        config.setRealm(source.getRealm());
        config.setUrl(source.getUrl());
        config.setType(source.getType());
        config.setNameServers(source.getNameServers());
        config.setPassword(source.getPassword());
        config.setVerifyKdcTrust(source.getVerifyKdcTrust());
        config.setTcpAllowed(source.getTcpAllowed());
        config.setPrincipal(source.getPrincipal());
        if (StringUtils.isNotBlank(source.getDomain())) {
            config.setDomain(source.getDomain());
        } else if (StringUtils.isNotBlank(source.getRealm())) {
            config.setDomain(source.getRealm().toLowerCase());
        }
        return config;
    }

}

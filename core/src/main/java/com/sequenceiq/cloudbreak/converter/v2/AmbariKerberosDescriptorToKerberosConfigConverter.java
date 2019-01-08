package com.sequenceiq.cloudbreak.converter.v2;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.AmbariKerberosDescriptor;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class AmbariKerberosDescriptorToKerberosConfigConverter extends AbstractConversionServiceAwareConverter<AmbariKerberosDescriptor, KerberosConfig> {

    @Override
    public KerberosConfig convert(AmbariKerberosDescriptor source) {
        KerberosConfig config = new KerberosConfig();
        config.setDescriptor(new String(Base64.decodeBase64(source.getDescriptor())));
        config.setKrb5Conf(new String(Base64.decodeBase64(source.getKrb5Conf())));
        config.setPrincipal(source.getPrincipal());
        config.setType(source.getType());
        config.setDomain(source.getDomain());
        config.setNameServers(source.getNameServers());
        config.setPassword(source.getPassword());
        config.setVerifyKdcTrust(source.getVerifyKdcTrust());
        config.setTcpAllowed(source.getTcpAllowed());
        return config;
    }

}

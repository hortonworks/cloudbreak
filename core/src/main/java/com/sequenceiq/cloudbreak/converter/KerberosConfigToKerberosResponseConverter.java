package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class KerberosConfigToKerberosResponseConverter extends AbstractConversionServiceAwareConverter<KerberosConfig, KerberosResponse> {
    @Override
    public KerberosResponse convert(KerberosConfig source) {
        KerberosResponse kerberosResponse = new KerberosResponse();
        kerberosResponse.setType(source.getType());
        kerberosResponse.setUrl(source.getUrl());
        kerberosResponse.setAdminUrl(source.getAdminUrl());
        kerberosResponse.setRealm(source.getRealm());
        kerberosResponse.setLdapUrl(source.getLdapUrl());
        kerberosResponse.setContainerDn(source.getContainerDn());
        kerberosResponse.setTcpAllowed(source.isTcpAllowed());
        return kerberosResponse;
    }
}

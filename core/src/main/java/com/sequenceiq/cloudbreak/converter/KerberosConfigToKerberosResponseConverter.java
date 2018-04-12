package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import org.springframework.stereotype.Component;

@Component
public class KerberosConfigToKerberosResponseConverter extends AbstractConversionServiceAwareConverter<KerberosConfig, KerberosResponse> {
    @Override
    public KerberosResponse convert(KerberosConfig source) {
        KerberosResponse kerberosResponse = new KerberosResponse();
        kerberosResponse.setType(source.getType());
        kerberosResponse.setAdmin(source.getAdmin());
        kerberosResponse.setUrl(source.getUrl());
        kerberosResponse.setAdminUrl(source.getAdminUrl());
        kerberosResponse.setRealm(source.getRealm());
        kerberosResponse.setDescriptor(source.getDescriptor());
        kerberosResponse.setKrb5Conf(source.getKrb5Conf());
        kerberosResponse.setLdapUrl(source.getLdapUrl());
        kerberosResponse.setContainerDn(source.getContainerDn());
        kerberosResponse.setTcpAllowed(source.getTcpAllowed());
        return kerberosResponse;
    }
}

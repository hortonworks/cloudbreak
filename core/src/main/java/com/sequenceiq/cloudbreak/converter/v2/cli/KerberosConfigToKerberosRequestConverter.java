package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class KerberosConfigToKerberosRequestConverter extends AbstractConversionServiceAwareConverter<KerberosConfig, KerberosRequest> {

    @Override
    public KerberosRequest convert(KerberosConfig source) {
        KerberosRequest config = new KerberosRequest();
        config.setType(source.getType());
        config.setTcpAllowed(source.isTcpAllowed());
        config.setPassword("");
        switch (source.getType()) {
            case CB_MANAGED:
                config.setMasterKey("");
                config.setAdmin("");
                break;
            case EXISTING_AD:
                config.setPrincipal("");
                config.setUrl(source.getUrl());
                config.setAdminUrl(source.getAdminUrl());
                config.setRealm(source.getRealm());
                config.setLdapUrl(source.getLdapUrl());
                config.setContainerDn(source.getContainerDn());
                break;
            case EXISTING_MIT:
                config.setPrincipal("");
                config.setUrl(source.getUrl());
                config.setAdminUrl(source.getAdminUrl());
                config.setRealm(source.getRealm());
                break;
            case CUSTOM:
                config.setPrincipal("");
                config.setDescriptor("");
                config.setKrb5Conf("");
                break;
            default:
        }
        return config;
    }

}

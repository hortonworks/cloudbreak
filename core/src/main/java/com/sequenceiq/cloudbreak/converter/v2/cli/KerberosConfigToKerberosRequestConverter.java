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
        config.setTcpAllowed(source.getTcpAllowed());
        config.setPassword("");
        switch (source.getType()) {
            case CB_MANAGED:
                config.setMasterKey("");
                config.setAdmin(source.getAdmin());
                break;
            case EXISTING_AD:
                config.setPrincipal(source.getPrincipal());
                config.setUrl(source.getUrl());
                config.setAdminUrl(source.getAdminUrl());
                config.setRealm(source.getRealm());
                config.setLdapUrl(source.getLdapUrl());
                config.setContainerDn(source.getContainerDn());
                break;
            case EXISTING_MIT:
                config.setPrincipal(source.getPrincipal());
                config.setUrl(source.getUrl());
                config.setAdminUrl(source.getAdminUrl());
                config.setRealm(source.getRealm());
                break;
            case CUSTOM:
                config.setPrincipal(source.getPrincipal());
                config.setDescriptor(source.getDescriptor());
                config.setKrb5Conf(source.getKrb5Conf());
                break;
            default:
        }
        return config;
    }

}

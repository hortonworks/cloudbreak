package com.sequenceiq.cloudbreak.converter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class KerberosRequestToKerberosConfigConverter extends AbstractConversionServiceAwareConverter<KerberosRequest, KerberosConfig> {
    @Override
    public KerberosConfig convert(KerberosRequest source) {
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setType(source.getType());
        kerberosConfig.setMasterKey(source.getMasterKey());
        kerberosConfig.setAdmin(source.getAdmin());
        kerberosConfig.setPassword(source.getPassword());
        kerberosConfig.setUrl(source.getUrl());
        kerberosConfig.setAdminUrl(Optional.ofNullable(source.getAdminUrl()).orElse(source.getUrl()));
        kerberosConfig.setRealm(source.getRealm());
        kerberosConfig.setTcpAllowed(source.getTcpAllowed());
        kerberosConfig.setVerifyKdcTrust(source.getVerifyKdcTrust());
        kerberosConfig.setPrincipal(source.getPrincipal());
        kerberosConfig.setLdapUrl(source.getLdapUrl());
        kerberosConfig.setContainerDn(source.getContainerDn());
        kerberosConfig.setDescriptor(source.getDescriptor());
        kerberosConfig.setKrb5Conf(source.getKrb5Conf());
        return kerberosConfig;
    }
}

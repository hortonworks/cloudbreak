package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class KerberosToKerberosRequestConverter extends AbstractConversionServiceAwareConverter<KerberosConfig, KerberosRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosToKerberosRequestConverter.class);

    @Override
    public KerberosRequest convert(KerberosConfig source) {
        KerberosRequest kerberosRequest = new KerberosRequest();
        kerberosRequest.setAdmin(source.getAdmin());
        kerberosRequest.setAdminUrl(source.getAdminUrl());
        kerberosRequest.setContainerDn(source.getContainerDn());
        kerberosRequest.setDescriptor(source.getDescriptor());
        kerberosRequest.setKrb5Conf(source.getKrb5Conf());
        kerberosRequest.setLdapUrl(source.getLdapUrl());
        kerberosRequest.setMasterKey(source.getMasterKey());
        kerberosRequest.setPassword(source.getPassword());
        kerberosRequest.setPrincipal(source.getPrincipal());
        kerberosRequest.setRealm(source.getRealm());
        kerberosRequest.setTcpAllowed(source.getTcpAllowed());
        return kerberosRequest;
    }

}

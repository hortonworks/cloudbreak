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
        kerberosRequest.setAdmin(source.getKerberosAdmin());
        kerberosRequest.setAdminUrl(source.getKdcAdminUrl());
        kerberosRequest.setContainerDn(source.getKerberosContainerDn());
        kerberosRequest.setDescriptor(source.getKerberosDescriptor());
        kerberosRequest.setKrb5Conf(source.getKrb5Conf());
        kerberosRequest.setLdapUrl(source.getKerberosLdapUrl());
        kerberosRequest.setMasterKey(source.getKerberosMasterKey());
        kerberosRequest.setPassword(source.getKerberosPassword());
        kerberosRequest.setPrincipal(source.getKerberosPrincipal());
        kerberosRequest.setRealm(source.getKerberosRealm());
        kerberosRequest.setTcpAllowed(source.getKerberosTcpAllowed());
        return kerberosRequest;
    }

}

package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class KerberosConfigToKerberosResponseConverter extends AbstractConversionServiceAwareConverter<KerberosConfig, KerberosResponse> {

    @Inject
    private ConversionService conversionService;

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
        kerberosResponse.setMasterKey(conversionService.convert(source.getMasterKeySecret(), SecretResponse.class));
        kerberosResponse.setAdmin(conversionService.convert(source.getAdminSecret(), SecretResponse.class));
        kerberosResponse.setPassword(conversionService.convert(source.getPasswordSecret(), SecretResponse.class));
        kerberosResponse.setPrincipal(conversionService.convert(source.getPrincipalSecret(), SecretResponse.class));
        kerberosResponse.setDescriptor(conversionService.convert(source.getDescriptorSecret(), SecretResponse.class));
        kerberosResponse.setKrb5Conf(conversionService.convert(source.getKrb5ConfSecret(), SecretResponse.class));
        kerberosResponse.setDomain(source.getDomain());
        kerberosResponse.setNameServers(source.getNameServers());
        return kerberosResponse;
    }
}

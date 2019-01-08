package com.sequenceiq.cloudbreak.converter;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class KerberosConfigToKerberosV4ResponseConverter extends AbstractConversionServiceAwareConverter<KerberosConfig, KerberosV4Response> {

    @Inject
    private ConversionService conversionService;

    @Override
    public KerberosV4Response convert(KerberosConfig source) {
        KerberosV4Response kerberosV4Response = new KerberosV4Response();
        kerberosV4Response.setType(source.getType());
        kerberosV4Response.setUrl(source.getUrl());
        kerberosV4Response.setAdminUrl(source.getAdminUrl());
        kerberosV4Response.setRealm(source.getRealm());
        kerberosV4Response.setLdapUrl(source.getLdapUrl());
        kerberosV4Response.setContainerDn(source.getContainerDn());
        kerberosV4Response.setTcpAllowed(source.isTcpAllowed());
        kerberosV4Response.setAdmin(conversionService.convert(source.getAdminSecret(), SecretResponse.class));
        kerberosV4Response.setPassword(conversionService.convert(source.getPasswordSecret(), SecretResponse.class));
        kerberosV4Response.setPrincipal(conversionService.convert(source.getPrincipalSecret(), SecretResponse.class));
        kerberosV4Response.setDescriptor(conversionService.convert(source.getDescriptorSecret(), SecretResponse.class));
        kerberosV4Response.setKrb5Conf(conversionService.convert(source.getKrb5ConfSecret(), SecretResponse.class));
        kerberosV4Response.setDomain(source.getDomain());
        kerberosV4Response.setNameServers(source.getNameServers());
        kerberosV4Response.setName(source.getName());
        kerberosV4Response.setDescription(source.getDescription());
        kerberosV4Response.setId(source.getId());
        kerberosV4Response.setVerifyKdcTrust(source.getVerifyKdcTrust());
        kerberosV4Response.setEnvironments(source.getEnvironments().stream().map(CompactView::getName).collect(Collectors.toSet()));
        return kerberosV4Response;
    }
}

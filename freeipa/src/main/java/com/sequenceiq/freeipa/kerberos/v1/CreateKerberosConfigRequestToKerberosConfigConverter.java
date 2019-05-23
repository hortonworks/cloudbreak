package com.sequenceiq.freeipa.kerberos.v1;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.kerberos.model.create.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.FreeIPAKerberosDescriptor;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.MITKerberosDescriptor;
import com.sequenceiq.freeipa.api.v1.kerberos.validation.KerberosRequestValidator;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;

@Component
public class CreateKerberosConfigRequestToKerberosConfigConverter {
    public KerberosConfig convert(CreateKerberosConfigRequest request) {
        if (request == null || !KerberosRequestValidator.isValid(request)) {
            throw new BadRequestException("Improper CreateKerberosConfigRequest!");
        }
        KerberosConfig kerberos;
        if (request.getFreeIpa() != null) {
            kerberos = convertFreeIpa(request.getFreeIpa());
        } else if (request.getActiveDirectory() != null) {
            kerberos = convertActiveDirectory(request.getActiveDirectory());
        } else if (request.getMit() != null) {
            kerberos = convertMit(request.getMit());
        } else {
            throw new BadRequestException("Unable to determine Kerberos Configuration since none of them are provided in the request!");
        }
        kerberos.setName(request.getName());
        kerberos.setDescription(request.getDescription());
        kerberos.setEnvironmentId(request.getEnvironmentId());
        return kerberos;
    }

    private KerberosConfig convertActiveDirectory(ActiveDirectoryKerberosDescriptor source) {
        KerberosConfig config = new KerberosConfig();
        config.setAdminUrl(source.getAdminUrl());
        config.setContainerDn(source.getContainerDn());
        config.setLdapUrl(source.getLdapUrl());
        config.setRealm(source.getRealm());
        config.setUrl(source.getUrl());
        config.setPrincipal(source.getPrincipal());
        config.setType(source.getType());
        config.setNameServers(source.getNameServers());
        config.setPassword(source.getPassword());
        config.setVerifyKdcTrust(source.getVerifyKdcTrust());
        config.setTcpAllowed(source.getTcpAllowed());
        if (StringUtils.isNotBlank(source.getDomain())) {
            config.setDomain(source.getDomain());
        } else if (StringUtils.isNotBlank(source.getRealm())) {
            config.setDomain(source.getRealm().toLowerCase());
        }
        return config;
    }

    private KerberosConfig convertMit(MITKerberosDescriptor source) {
        KerberosConfig config = new KerberosConfig();
        config.setAdminUrl(source.getAdminUrl());
        config.setRealm(source.getRealm());
        config.setUrl(source.getUrl());
        config.setPrincipal(source.getPrincipal());
        config.setType(source.getType());
        config.setDomain(source.getDomain());
        config.setNameServers(source.getNameServers());
        config.setPassword(source.getPassword());
        config.setVerifyKdcTrust(source.getVerifyKdcTrust());
        config.setTcpAllowed(source.getTcpAllowed());
        return config;
    }

    private KerberosConfig convertFreeIpa(FreeIPAKerberosDescriptor source) {
        KerberosConfig config = new KerberosConfig();
        config.setAdminUrl(source.getAdminUrl());
        config.setRealm(source.getRealm());
        config.setUrl(source.getUrl());
        config.setType(source.getType());
        config.setNameServers(source.getNameServers());
        config.setPassword(source.getPassword());
        config.setVerifyKdcTrust(source.getVerifyKdcTrust());
        config.setTcpAllowed(source.getTcpAllowed());
        config.setPrincipal(source.getPrincipal());
        if (StringUtils.isNotBlank(source.getDomain())) {
            config.setDomain(source.getDomain());
        } else if (StringUtils.isNotBlank(source.getRealm())) {
            config.setDomain(source.getRealm().toLowerCase());
        }
        return config;
    }
}

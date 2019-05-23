package com.sequenceiq.freeipa.kerberos.v1;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;

@Service
public class KerberosConfigV1Service {
    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private CreateKerberosConfigRequestToKerberosConfigConverter createKerberosConfigRequestToKerberosConfigConverter;

    @Inject
    private KerberosConfigToCreateKerberosConfigRequestConverter kerberosConfigToCreateKerberosConfigRequestConverter;

    @Inject
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    public DescribeKerberosConfigResponse post(CreateKerberosConfigRequest createKerberosConfigRequest) {
        KerberosConfig kerberosConfig = createKerberosConfigRequestToKerberosConfigConverter.convert(createKerberosConfigRequest);
        kerberosConfig = kerberosConfigService.createKerberosConfig(kerberosConfig);
        return convertKerberosConfigToDescribeKerberosConfigResponse(kerberosConfig);
    }

    public DescribeKerberosConfigResponse describe(String environmentId) {
        KerberosConfig kerberosConfig = kerberosConfigService.get(environmentId);
        return convertKerberosConfigToDescribeKerberosConfigResponse(kerberosConfig);
    }

    public void delete(String environmentId) {
        kerberosConfigService.delete(environmentId);
    }

    public CreateKerberosConfigRequest getCreateRequest(String environmentId) {
        return kerberosConfigToCreateKerberosConfigRequestConverter.convert(kerberosConfigService.get(environmentId));
    }

    private DescribeKerberosConfigResponse convertKerberosConfigToDescribeKerberosConfigResponse(KerberosConfig source) {
        DescribeKerberosConfigResponse describeKerberosConfigResponse = new DescribeKerberosConfigResponse();
        describeKerberosConfigResponse.setType(source.getType());
        describeKerberosConfigResponse.setUrl(source.getUrl());
        describeKerberosConfigResponse.setAdminUrl(source.getAdminUrl());
        describeKerberosConfigResponse.setRealm(source.getRealm());
        describeKerberosConfigResponse.setLdapUrl(source.getLdapUrl());
        describeKerberosConfigResponse.setContainerDn(source.getContainerDn());
        describeKerberosConfigResponse.setTcpAllowed(source.isTcpAllowed());
        describeKerberosConfigResponse.setAdmin(stringToSecretResponseConverter.convert(source.getAdminSecret()));
        describeKerberosConfigResponse.setPassword(stringToSecretResponseConverter.convert(source.getPasswordSecret()));
        describeKerberosConfigResponse.setPrincipal(stringToSecretResponseConverter.convert(source.getPrincipalSecret()));
        describeKerberosConfigResponse.setDescriptor(stringToSecretResponseConverter.convert(source.getDescriptorSecret()));
        describeKerberosConfigResponse.setKrb5Conf(stringToSecretResponseConverter.convert(source.getKrb5ConfSecret()));
        describeKerberosConfigResponse.setDomain(source.getDomain());
        describeKerberosConfigResponse.setNameServers(source.getNameServers());
        describeKerberosConfigResponse.setName(source.getName());
        describeKerberosConfigResponse.setDescription(source.getDescription());
        describeKerberosConfigResponse.setId(source.getResourceCrn());
        describeKerberosConfigResponse.setVerifyKdcTrust(source.getVerifyKdcTrust());
        describeKerberosConfigResponse.setEnvironmentId(source.getEnvironmentId());
        return describeKerberosConfigResponse;
    }
}

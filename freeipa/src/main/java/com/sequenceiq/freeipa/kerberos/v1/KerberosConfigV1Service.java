package com.sequenceiq.freeipa.kerberos.v1;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.service.config.KerberosConfigRegisterService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaPermissionService;
import com.sequenceiq.freeipa.service.stack.StackService;

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

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private KerberosConfigRegisterService kerberosConfigRegisterService;

    @Inject
    private FreeIpaPermissionService freeIpaPermissionService;

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
        describeKerberosConfigResponse.setPassword(stringToSecretResponseConverter.convert(source.getPasswordSecret()));
        describeKerberosConfigResponse.setPrincipal(stringToSecretResponseConverter.convert(source.getPrincipalSecret()));
        describeKerberosConfigResponse.setDescriptor(stringToSecretResponseConverter.convert(source.getDescriptorSecret()));
        describeKerberosConfigResponse.setKrb5Conf(stringToSecretResponseConverter.convert(source.getKrb5ConfSecret()));
        describeKerberosConfigResponse.setDomain(source.getDomain());
        describeKerberosConfigResponse.setNameServers(source.getNameServers());
        describeKerberosConfigResponse.setName(source.getName());
        describeKerberosConfigResponse.setDescription(source.getDescription());
        describeKerberosConfigResponse.setCrn(source.getResourceCrn());
        describeKerberosConfigResponse.setVerifyKdcTrust(source.getVerifyKdcTrust());
        describeKerberosConfigResponse.setEnvironmentCrn(source.getEnvironmentCrn());
        return describeKerberosConfigResponse;
    }

    public DescribeKerberosConfigResponse getForCluster(String environmentCrn, String accountId, String clusterName) throws FreeIpaClientException {
        Optional<Stack> stack = stackService.findByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (stack.isPresent()) {
            Optional<KerberosConfig> existingKerberosConfig = kerberosConfigService.find(environmentCrn, accountId, clusterName);
            KerberosConfig kerberosConfig;
            if (existingKerberosConfig.isPresent()) {
                kerberosConfig = existingKerberosConfig.get();
            } else {
                FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack.get());
                String bindUser = "kerberosbind-" + clusterName;
                Optional<User> existinguser = freeIpaClient.userFind(bindUser);
                User user = existinguser.isPresent() ? existinguser.get() : freeIpaClient.userAdd(bindUser, "service", "account");
                String password = PasswordUtil.generatePassword();
                freeIpaClient.userSetPassword(user.getUid(), password);
                freeIpaClient.addRoleMember("Enrollment Administrator", Set.of(user.getUid()), null, null, null, null);
                freeIpaPermissionService.setPermissions(freeIpaClient);
                kerberosConfig = kerberosConfigRegisterService.createKerberosConfig(stack.get().getId(), user.getUid(), password, clusterName);
            }
            return convertKerberosConfigToDescribeKerberosConfigResponse(kerberosConfig);
        } else {
            return describe(environmentCrn);
        }
    }
}

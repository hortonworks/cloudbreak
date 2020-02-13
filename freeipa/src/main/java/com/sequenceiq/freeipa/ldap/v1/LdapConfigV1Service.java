package com.sequenceiq.freeipa.ldap.v1;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.util.FreeIpaPasswordUtil;
import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;
import com.sequenceiq.freeipa.api.v1.ldap.model.create.CreateLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.MinimalLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.service.config.LdapConfigRegisterService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class LdapConfigV1Service {
    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private LdapConfigRegisterService ldapConfigRegisterService;

    public DescribeLdapConfigResponse post(CreateLdapConfigRequest createLdapConfigRequest) {
        LdapConfig ldapConfig = convertCreateLdapConfigRequest(createLdapConfigRequest);
        ldapConfig = ldapConfigService.createLdapConfig(ldapConfig);
        return convertLdapConfigToDescribeLdapConfigResponse(ldapConfig);
    }

    public DescribeLdapConfigResponse describe(String environmentId) {
        LdapConfig ldapConfig = ldapConfigService.get(environmentId);
        return convertLdapConfigToDescribeLdapConfigResponse(ldapConfig);
    }

    public void delete(String environmentId) {
        ldapConfigService.delete(environmentId);
    }

    public CreateLdapConfigRequest getCreateLdapConfigRequest(String environmentId) {
        return convertLdapConfigToCreateLdapConfigRequest(ldapConfigService.get(environmentId));
    }

    public TestLdapConfigResponse testConnection(TestLdapConfigRequest testLdapConfigRequest) {
        LdapConfig ldapConfig = null;
        if (testLdapConfigRequest.getLdap() != null) {
            ldapConfig = convertMinimalLdapConfigRequestToLdapConfig(testLdapConfigRequest.getLdap());
        }
        String result = ldapConfigService.testConnection(testLdapConfigRequest.getEnvironmentCrn(), ldapConfig);
        TestLdapConfigResponse testLdapConfigResponse = new TestLdapConfigResponse();
        testLdapConfigResponse.setResult(result);
        return testLdapConfigResponse;
    }

    private LdapConfig convertCreateLdapConfigRequest(CreateLdapConfigRequest createLdapConfigRequest) {
        LdapConfig config = new LdapConfig();
        if (Strings.isNullOrEmpty(createLdapConfigRequest.getName())) {
            config.setName(missingResourceNameGenerator.generateName(APIResourceType.LDAP_CONFIG));
        } else {
            config.setName(createLdapConfigRequest.getName());
        }
        config.setDescription(createLdapConfigRequest.getDescription());
        config.setEnvironmentCrn(createLdapConfigRequest.getEnvironmentCrn());
        config.setBindDn(createLdapConfigRequest.getBindDn());
        config.setBindPassword(createLdapConfigRequest.getBindPassword());
        config.setServerHost(createLdapConfigRequest.getHost());
        config.setServerPort(createLdapConfigRequest.getPort());
        config.setProtocol(createLdapConfigRequest.getProtocol());
        config.setGroupSearchBase(createLdapConfigRequest.getGroupSearchBase());
        config.setUserSearchBase(createLdapConfigRequest.getUserSearchBase());
        config.setUserDnPattern(createLdapConfigRequest.getUserDnPattern());
        config.setUserNameAttribute(createLdapConfigRequest.getUserNameAttribute());
        config.setDomain(createLdapConfigRequest.getDomain());
        config.setDirectoryType(createLdapConfigRequest.getDirectoryType() != null ? createLdapConfigRequest.getDirectoryType() : DirectoryType.LDAP);
        config.setUserObjectClass(createLdapConfigRequest.getUserObjectClass() != null ? createLdapConfigRequest.getUserObjectClass() : "person");
        config.setGroupObjectClass(createLdapConfigRequest.getGroupObjectClass() != null ? createLdapConfigRequest.getGroupObjectClass() : "groupOfNames");
        config.setGroupNameAttribute(createLdapConfigRequest.getGroupNameAttribute() != null ? createLdapConfigRequest.getGroupNameAttribute() : "cn");
        config.setGroupMemberAttribute(createLdapConfigRequest.getGroupMemberAttribute() != null ? createLdapConfigRequest.getGroupMemberAttribute() : "member");
        config.setAdminGroup(createLdapConfigRequest.getAdminGroup());
        config.setCertificate(createLdapConfigRequest.getCertificate());
        return config;
    }

    private DescribeLdapConfigResponse convertLdapConfigToDescribeLdapConfigResponse(LdapConfig config) {
        DescribeLdapConfigResponse describeLdapConfigResponse = new DescribeLdapConfigResponse();
        describeLdapConfigResponse.setName(config.getName());
        describeLdapConfigResponse.setDescription(config.getDescription());
        describeLdapConfigResponse.setCrn(config.getResourceCrn());
        describeLdapConfigResponse.setHost(config.getServerHost());
        describeLdapConfigResponse.setPort(config.getServerPort());
        describeLdapConfigResponse.setProtocol(config.getProtocol());
        describeLdapConfigResponse.setBindDn(stringToSecretResponseConverter.convert(config.getBindDnSecret()));
        describeLdapConfigResponse.setBindPassword(stringToSecretResponseConverter.convert(config.getBindPasswordSecret()));
        describeLdapConfigResponse.setGroupSearchBase(config.getGroupSearchBase());
        describeLdapConfigResponse.setUserSearchBase(config.getUserSearchBase());
        describeLdapConfigResponse.setUserDnPattern(config.getUserDnPattern());
        describeLdapConfigResponse.setUserNameAttribute(config.getUserNameAttribute());
        describeLdapConfigResponse.setDomain(config.getDomain());
        describeLdapConfigResponse.setDirectoryType(config.getDirectoryType());
        describeLdapConfigResponse.setUserObjectClass(config.getUserObjectClass());
        describeLdapConfigResponse.setGroupObjectClass(config.getGroupObjectClass());
        describeLdapConfigResponse.setGroupNameAttribute(config.getGroupNameAttribute());
        describeLdapConfigResponse.setGroupMemberAttribute(config.getGroupMemberAttribute());
        describeLdapConfigResponse.setAdminGroup(config.getAdminGroup());
        describeLdapConfigResponse.setUserGroup(config.getUserGroup());
        describeLdapConfigResponse.setCertificate(config.getCertificate());
        describeLdapConfigResponse.setEnvironmentCrn(config.getEnvironmentCrn());
        return describeLdapConfigResponse;
    }

    private LdapConfig convertMinimalLdapConfigRequestToLdapConfig(MinimalLdapConfigRequest minimalLdapConfigRequest) {
        LdapConfig config = new LdapConfig();
        config.setBindDn(minimalLdapConfigRequest.getBindDn());
        config.setBindPassword(minimalLdapConfigRequest.getBindPassword());
        config.setServerHost(minimalLdapConfigRequest.getHost());
        config.setServerPort(minimalLdapConfigRequest.getPort());
        config.setProtocol(minimalLdapConfigRequest.getProtocol());
        return config;
    }

    private CreateLdapConfigRequest convertLdapConfigToCreateLdapConfigRequest(LdapConfig source) {
        CreateLdapConfigRequest createLdapConfigRequest = new CreateLdapConfigRequest();
        createLdapConfigRequest.setName(source.getName());
        createLdapConfigRequest.setEnvironmentCrn(source.getEnvironmentCrn());
        createLdapConfigRequest.setBindDn("fake-user");
        createLdapConfigRequest.setBindPassword("fake-password");
        createLdapConfigRequest.setAdminGroup(source.getAdminGroup());
        createLdapConfigRequest.setDescription(source.getDescription());
        createLdapConfigRequest.setDirectoryType(source.getDirectoryType());
        createLdapConfigRequest.setDomain(source.getDomain());
        createLdapConfigRequest.setGroupMemberAttribute(source.getGroupMemberAttribute());
        createLdapConfigRequest.setGroupNameAttribute(source.getGroupNameAttribute());
        createLdapConfigRequest.setGroupObjectClass(source.getGroupObjectClass());
        createLdapConfigRequest.setGroupSearchBase(source.getGroupSearchBase());
        createLdapConfigRequest.setProtocol(source.getProtocol());
        createLdapConfigRequest.setHost(source.getServerHost());
        createLdapConfigRequest.setPort(source.getServerPort());
        createLdapConfigRequest.setUserDnPattern(source.getUserDnPattern());
        createLdapConfigRequest.setUserNameAttribute(source.getUserNameAttribute());
        createLdapConfigRequest.setUserObjectClass(source.getUserObjectClass());
        createLdapConfigRequest.setUserSearchBase(source.getUserSearchBase());
        createLdapConfigRequest.setCertificate(source.getCertificate());
        return createLdapConfigRequest;
    }

    public DescribeLdapConfigResponse getForCluster(String environmentCrn, String accountId, String clusterName)
            throws FreeIpaClientException {
        Optional<Stack> stack = stackService.findByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (stack.isPresent()) {
            Optional<LdapConfig> existingLdapConfig = ldapConfigService.find(environmentCrn, accountId, clusterName);
            LdapConfig ldapConfig;
            if (existingLdapConfig.isPresent()) {
                ldapConfig = existingLdapConfig.get();
            } else {
                FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack.get());
                String bindUser = "ldapbind-" + clusterName;
                Optional<User> existinguser = freeIpaClient.userFind(bindUser);
                User user = existinguser.isPresent() ? existinguser.get() : freeIpaClient.userAdd(bindUser, "service", "account");
                String password = FreeIpaPasswordUtil.generatePassword();
                freeIpaClient.userSetPasswordWithExpiration(user.getUid(), password, Optional.empty());
                ldapConfig = ldapConfigRegisterService.createLdapConfig(stack.get().getId(), user.getDn(), password, clusterName, environmentCrn);
            }
            return convertLdapConfigToDescribeLdapConfigResponse(ldapConfig);
        } else {
            return describe(environmentCrn);
        }
    }
}

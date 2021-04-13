package com.sequenceiq.freeipa.ldap.v1;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
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
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.service.config.LdapConfigRegisterService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class LdapConfigV1Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigV1Service.class);

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

    public DescribeLdapConfigResponse describe(String environmentCrn) {
        LdapConfig ldapConfig = ldapConfigService.get(environmentCrn);
        return convertLdapConfigToDescribeLdapConfigResponse(ldapConfig);
    }

    public void delete(String environmentCrn) {
        ldapConfigService.delete(environmentCrn);
    }

    public CreateLdapConfigRequest getCreateLdapConfigRequest(String environmentCrn) {
        return convertLdapConfigToCreateLdapConfigRequest(ldapConfigService.get(environmentCrn));
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

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public DescribeLdapConfigResponse getForCluster(String environmentCrn, String accountId, String clusterName) throws FreeIpaClientException {
        Optional<Stack> stack = stackService.findByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (stack.isPresent()) {
            return getLdapConfigIfFreeIPAExists(environmentCrn, accountId, clusterName, stack.get());
        } else {
            LOGGER.debug("No FreeIPA for environment, try to fetch manually registered LDAP configuration");
            return describe(environmentCrn);
        }
    }

    private DescribeLdapConfigResponse getLdapConfigIfFreeIPAExists(String environmentCrn, String accountId, String clusterName, Stack stack)
            throws FreeIpaClientException {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("FreeIPA exists for environment");
        Optional<LdapConfig> existingLdapConfig = ldapConfigService.find(environmentCrn, accountId, clusterName);
        LdapConfig ldapConfig;
        if (existingLdapConfig.isPresent()) {
            LOGGER.debug("LdapConfig already exists");
            ldapConfig = existingLdapConfig.get();
        } else {
            throw NotFoundException.notFoundException("LDAP config", clusterName);
        }
        return convertLdapConfigToDescribeLdapConfigResponse(ldapConfig);
    }

    public LdapConfig createNewLdapConfig(String environmentCrn, String clusterName, Stack stack, boolean ignoreExistingUser) throws FreeIpaClientException {
        LOGGER.debug("Create new LDAP config for environment in FreeIPA");
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        User user = ignoreExistingUser ? createBindUserIgnoreExisting(clusterName, freeIpaClient) : createBindUser(clusterName, freeIpaClient);
        String password = setBindUserPassword(freeIpaClient, user);
        return ldapConfigRegisterService.createLdapConfig(stack.getId(), user.getDn(), password, clusterName, environmentCrn);
    }

    private String setBindUserPassword(FreeIpaClient freeIpaClient, User user) throws FreeIpaClientException {
        String password = FreeIpaPasswordUtil.generatePassword();
        freeIpaClient.userSetPasswordWithExpiration(user.getUid(), password, Optional.empty());
        LOGGER.debug("Password is set for user: [{}]", user.getUid());
        return password;
    }

    private User createBindUser(String clusterName, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        String bindUser = createBindUserName(clusterName);
        try {
            return freeIpaClient.userAdd(bindUser, "service", "account");
        } catch (FreeIpaClientException e) {
            LOGGER.warn("Failed to create LDAP bind user: [{}]", bindUser, e);
            throw new RetryableFreeIpaClientException("Failed to create LDAP bind user", e);
        }
    }

    private User createBindUserIgnoreExisting(String clusterName, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        String bindUser = createBindUserName(clusterName);
        try {
            return freeIpaClient.userAdd(bindUser, "service", "account");
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isDuplicateEntryException(e)) {
                return getExistingBindUser(freeIpaClient, bindUser);
            } else {
                LOGGER.warn("Failed to create LDAP bind user: [{}]", bindUser, e);
                throw new RetryableFreeIpaClientException("Failed to create LDAP bind user", e);
            }
        }
    }

    private User getExistingBindUser(FreeIpaClient freeIpaClient, String bindUser) throws RetryableFreeIpaClientException {
        try {
            LOGGER.info("Bind user already exists, getting it from FreeIPA");
            return freeIpaClient.userShow(bindUser);
        } catch (FreeIpaClientException e) {
            LOGGER.warn("Failed to get kerberos bind user: [{}]", bindUser, e);
            throw new RetryableFreeIpaClientException("Failed to get kerberos bind user", e);
        }
    }

    private String createBindUserName(String clusterName) {
        return "ldapbind-" + clusterName;
    }
}

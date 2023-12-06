package com.sequenceiq.freeipa.ldap.v1;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.util.FreeIpaPasswordUtil;
import com.sequenceiq.freeipa.api.v1.ldap.model.create.CreateLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigConverter;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.service.binduser.LdapBindUserNameProvider;
import com.sequenceiq.freeipa.service.config.LdapConfigRegisterService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class LdapConfigV1Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigV1Service.class);

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private LdapConfigRegisterService ldapConfigRegisterService;

    @Inject
    private LdapBindUserNameProvider userNameProvider;

    @Inject
    private LdapConfigConverter ldapConfigConverter;

    public DescribeLdapConfigResponse post(CreateLdapConfigRequest createLdapConfigRequest) {
        LdapConfig ldapConfig = ldapConfigConverter.convertCreateLdapConfigRequest(createLdapConfigRequest);
        LdapConfig persistedLdapConfig = ldapConfigService.createLdapConfig(ldapConfig);
        return ldapConfigConverter.convertLdapConfigToDescribeLdapConfigResponse(persistedLdapConfig);
    }

    public DescribeLdapConfigResponse describe(String environmentCrn) {
        LdapConfig ldapConfig = ldapConfigService.get(environmentCrn);
        return ldapConfigConverter.convertLdapConfigToDescribeLdapConfigResponse(ldapConfig);
    }

    public void delete(String environmentCrn) {
        ldapConfigService.delete(environmentCrn);
    }

    public CreateLdapConfigRequest getCreateLdapConfigRequest(String environmentCrn) {
        return ldapConfigConverter.convertLdapConfigToCreateLdapConfigRequest(ldapConfigService.get(environmentCrn));
    }

    public TestLdapConfigResponse testConnection(TestLdapConfigRequest testLdapConfigRequest) {
        LdapConfig ldapConfig = testLdapConfigRequest.getLdap() != null ?
                ldapConfigConverter.convertMinimalLdapConfigRequestToLdapConfig(testLdapConfigRequest.getLdap()) : null;
        String result = ldapConfigService.testConnection(testLdapConfigRequest.getEnvironmentCrn(), ldapConfig);
        TestLdapConfigResponse testLdapConfigResponse = new TestLdapConfigResponse();
        testLdapConfigResponse.setResult(result);
        return testLdapConfigResponse;
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
            ldapConfig = createNewLdapConfig(environmentCrn, clusterName, stack, false);
        }
        return ldapConfigConverter.convertLdapConfigToDescribeLdapConfigResponse(ldapConfig);
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public LdapConfig createNewLdapConfig(String environmentCrn, String clusterName, Stack stack, boolean ignoreExistingUser) throws FreeIpaClientException {
        LOGGER.debug("Create new LDAP config for environment in FreeIPA");
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        User user = ignoreExistingUser ? createBindUserIgnoreExisting(clusterName, freeIpaClient) : createBindUser(clusterName, freeIpaClient);
        String password = setBindUserPassword(freeIpaClient, user);
        return ldapConfigRegisterService.createLdapConfig(stack.getId(), user.getDn(), password, clusterName, environmentCrn);
    }

    public String setBindUserPassword(FreeIpaClient freeIpaClient, User user, String password) throws FreeIpaClientException {
        freeIpaClient.userSetPasswordWithExpiration(user.getUid(), password, Optional.empty());
        LOGGER.debug("Password is set for user: [{}]", user.getUid());
        return password;
    }

    private String setBindUserPassword(FreeIpaClient freeIpaClient, User user) throws FreeIpaClientException {
        return setBindUserPassword(freeIpaClient, user, FreeIpaPasswordUtil.generatePassword());
    }

    private User createBindUser(String clusterName, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        String bindUser = userNameProvider.createBindUserName(clusterName);
        try {
            return freeIpaClient.userAdd(bindUser, "service", "account");
        } catch (FreeIpaClientException e) {
            LOGGER.warn("Failed to create LDAP bind user: [{}]", bindUser, e);
            throw new RetryableFreeIpaClientException("Failed to create LDAP bind user", e);
        }
    }

    private User createBindUserIgnoreExisting(String clusterName, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        String bindUser = userNameProvider.createBindUserName(clusterName);
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
}

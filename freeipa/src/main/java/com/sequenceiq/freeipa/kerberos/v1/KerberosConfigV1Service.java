package com.sequenceiq.freeipa.kerberos.v1;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.util.FreeIpaPasswordUtil;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.service.config.KerberosConfigRegisterService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class KerberosConfigV1Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosConfigV1Service.class);

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

    public DescribeKerberosConfigResponse post(CreateKerberosConfigRequest createKerberosConfigRequest) {
        KerberosConfig kerberosConfig = createKerberosConfigRequestToKerberosConfigConverter.convert(createKerberosConfigRequest);
        kerberosConfig = kerberosConfigService.createKerberosConfig(kerberosConfig);
        return convertKerberosConfigToDescribeKerberosConfigResponse(kerberosConfig);
    }

    public DescribeKerberosConfigResponse describe(String environmentCrn) {
        KerberosConfig kerberosConfig = kerberosConfigService.get(environmentCrn);
        return convertKerberosConfigToDescribeKerberosConfigResponse(kerberosConfig);
    }

    public void delete(String environmentCrn) {
        kerberosConfigService.delete(environmentCrn);
    }

    public CreateKerberosConfigRequest getCreateRequest(String environmentCrn) {
        return kerberosConfigToCreateKerberosConfigRequestConverter.convert(kerberosConfigService.get(environmentCrn));
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

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public DescribeKerberosConfigResponse getForCluster(String environmentCrn, String accountId, String clusterName) throws FreeIpaClientException {
        Optional<Stack> stack = stackService.findByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (stack.isPresent()) {
            return getKerberosConfigIfFreeIPAExists(environmentCrn, accountId, clusterName, stack.get());
        } else {
            LOGGER.debug("No FreeIPA found for env, try to look for manually registered Kerberos configuration");
            return describe(environmentCrn);
        }
    }

    private DescribeKerberosConfigResponse getKerberosConfigIfFreeIPAExists(String environmentCrn, String accountId, String clusterName, Stack stack)
            throws FreeIpaClientException {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Get kerberos config when FreeIPA exists for env");
        Optional<KerberosConfig> existingKerberosConfig = kerberosConfigService.find(environmentCrn, accountId, clusterName);
        KerberosConfig kerberosConfig;
        if (existingKerberosConfig.isPresent()) {
            LOGGER.debug("Kerberos config already exists");
            kerberosConfig = existingKerberosConfig.get();
        } else {
            throw NotFoundException.notFoundException("Kerberos config", clusterName);
        }
        return convertKerberosConfigToDescribeKerberosConfigResponse(kerberosConfig);
    }

    public KerberosConfig createNewKerberosConfig(String environmentCrn, String clusterName, Stack existingStack, boolean ignoreExistingUser)
            throws FreeIpaClientException {
        LOGGER.debug("Kerberos config doesn't exists for cluster [{}] in env [{}]. Creating new in FreeIPA", clusterName, environmentCrn);
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(existingStack);
        User user = ignoreExistingUser ? createBindUserIgnoreExisting(freeIpaClient, clusterName) : createBindUser(freeIpaClient, clusterName);
        String password = setPasswordForBindUser(freeIpaClient, user);
        addEnrollmentAdminRole(freeIpaClient, user);
        return kerberosConfigRegisterService.createKerberosConfig(existingStack.getId(), user.getUid(), password, clusterName, environmentCrn);
    }

    private void addEnrollmentAdminRole(FreeIpaClient freeIpaClient, User user) throws FreeIpaClientException {
        freeIpaClient.addRoleMember("Enrollment Administrator", Set.of(user.getUid()), null, null, null, null);
        LOGGER.debug("Added [Enrollment Administrator] role to [{}]", user.getUid());
    }

    private String setPasswordForBindUser(FreeIpaClient freeIpaClient, User user) throws FreeIpaClientException {
        String password = FreeIpaPasswordUtil.generatePassword();
        freeIpaClient.userSetPasswordWithExpiration(user.getUid(), password, Optional.empty());
        LOGGER.debug("Password is set  for [{}]", user.getUid());
        return password;
    }

    private User createBindUser(FreeIpaClient freeIpaClient, String clusterName) throws RetryableFreeIpaClientException {
        String bindUser = createBindUserName(clusterName);
        try {
            return freeIpaClient.userAdd(bindUser, "service", "account");
        } catch (FreeIpaClientException e) {
            LOGGER.warn("Failed to create kerberos bind user: [{}]", bindUser, e);
            throw new RetryableFreeIpaClientException("Failed to create kerberos bind user", e);
        }

    }

    private User createBindUserIgnoreExisting(FreeIpaClient freeIpaClient, String clusterName) throws RetryableFreeIpaClientException {
        String bindUser = createBindUserName(clusterName);
        try {
            return freeIpaClient.userAdd(bindUser, "service", "account");
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isDuplicateEntryException(e)) {
                return getExistingBindUser(freeIpaClient, bindUser);
            } else {
                LOGGER.warn("Failed to create kerberos bind user: [{}]", bindUser, e);
                throw new RetryableFreeIpaClientException("Failed to create kerberos bind user", e);
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
        return "kerberosbind-" + clusterName;
    }
}

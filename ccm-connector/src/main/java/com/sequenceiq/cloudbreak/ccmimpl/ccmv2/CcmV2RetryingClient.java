package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse;
import com.sequenceiq.cloudbreak.ccm.exception.CcmV2Exception;

@Component
public class CcmV2RetryingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmV2RetryingClient.class);

    @Inject
    private CcmV2Client ccmV2Client;

    @Inject
    private RetryTemplateFactory retryTemplateFactory;

    public InvertingProxy awaitReadyInvertingProxyForAccount(String accountId) {
        return retryTemplateFactory.getRetryTemplate().execute(
                retryContext -> ccmV2Client.getOrCreateInvertingProxy(accountId),
                retryExhausted -> {
                    LOGGER.error("Error Retrieving InvertingProxy for accountId '{}', retryCount '{}'", accountId, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error Retrieving CCM InvertingProxy", retryExhausted.getLastThrowable());
                });
    }

    public InvertingProxyAgent registerInvertingProxyAgent(String accountId, Optional<String> environmentCrnOpt,
        String domainName, String keyId, Optional<String> hmacKeyOpt) {
        return retryTemplateFactory.getRetryTemplate().execute(
                retryContext -> ccmV2Client.registerAgent(accountId, environmentCrnOpt, domainName, keyId, hmacKeyOpt),
                retryExhausted -> {
                    LOGGER.error("Error Registering Agent for accountId '{}', domainName '{}', retryCount '{}'",
                            accountId, domainName, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error Registering CCM Agent", retryExhausted.getLastThrowable());
                });
    }

    public UnregisterAgentResponse deregisterInvertingProxyAgent(String agentCrn) {
        return retryTemplateFactory.getRetryTemplate().execute(
                retryContext -> ccmV2Client.deregisterAgent(agentCrn),
                retryExhausted -> {
                    LOGGER.error("Error Deregistering Agent for agentCrn '{}', retryCount '{}'", agentCrn, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error Deregistering CCM Agent", retryExhausted.getLastThrowable());
                });
    }

    public List<InvertingProxyAgent> listInvertingProxyAgents(String accountId, Optional<String> environmentCrnOpt) {
        return retryTemplateFactory.getRetryTemplate().execute(
                retryContext -> ccmV2Client.listAgents(accountId, environmentCrnOpt),
                retryExhausted -> {
                    LOGGER.error("Error Listing Agents for account '{}' and environmentCrn '{}', retryCount '{}'",
                            accountId, environmentCrnOpt, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error Listing CCM Agents", retryExhausted.getLastThrowable());
                });
    }

    public InvertingProxyAgent createAgentAccessKeyPair(String accountId, String agentCrn, Optional<String> hmacKey) {
        return retryTemplateFactory.getRetryTemplate().execute(
                retryContext -> ccmV2Client.createAgentAccessKeyPair(accountId, agentCrn, hmacKey),
                retryExhausted -> {
                    LOGGER.error("Error creating agent access key pair for account '{}' and agentCrn '{}', retryCount '{}'",
                            accountId, agentCrn, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error creating agent access key pair", retryExhausted.getLastThrowable());
                });
    }

    public DeactivateAgentAccessKeyPairResponse deactivateAgentAccessKeyPair(String accountId, String accessKeyId) {
        return retryTemplateFactory.getRetryTemplate().execute(
                retryContext -> ccmV2Client.deactivateAgentAccessKeyPair(accountId, accessKeyId),
                retryExhausted -> {
                    LOGGER.error("Error deactivating agent access key pair for account '{}' and accessKeyId '{}', retryCount '{}'",
                            accountId, accessKeyId, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error deactivating agent access key pair", retryExhausted.getLastThrowable());
                });
    }

}

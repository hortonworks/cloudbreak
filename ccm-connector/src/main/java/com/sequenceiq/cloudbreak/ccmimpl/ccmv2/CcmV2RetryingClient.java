package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    public InvertingProxy awaitReadyInvertingProxyForAccount(String requestId, String accountId) {
        return retryTemplateFactory.getRetryTemplate().execute(
                retryContext -> ccmV2Client.getOrCreateInvertingProxy(requestId, accountId),
                retryExhausted -> {
                    LOGGER.error("Error Retrieving InvertingProxy for accountId '{}', retryCount '{}'", accountId, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error Retrieving CCM InvertingProxy", retryExhausted.getLastThrowable());
                });
    }

    public InvertingProxyAgent registerInvertingProxyAgent(String requestId, String accountId, Optional<String> environmentCrnOpt,
        String domainName, String keyId, Optional<String> hmacKeyOpt) {
        return retryTemplateFactory.getRetryTemplate().execute(
                retryContext -> ccmV2Client.registerAgent(requestId, accountId, environmentCrnOpt, domainName, keyId, hmacKeyOpt),
                retryExhausted -> {
                    LOGGER.error("Error Registering Agent for accountId '{}', domainName '{}', retryCount '{}'",
                            accountId, domainName, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error Registering CCM Agent", retryExhausted.getLastThrowable());
                });
    }

    public UnregisterAgentResponse deregisterInvertingProxyAgent(String requestId, String agentCrn) {
        return retryTemplateFactory.getRetryTemplate().execute(
                retryContext -> ccmV2Client.deregisterAgent(requestId, agentCrn),
                retryExhausted -> {
                    LOGGER.error("Error Deregistering Agent for agentCrn '{}', retryCount '{}'", agentCrn, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error Deregistering CCM Agent", retryExhausted.getLastThrowable());
                });
    }

    public List<InvertingProxyAgent> listInvertingProxyAgents(String requestId, String accountId, Optional<String> environmentCrnOpt) {
        return retryTemplateFactory.getRetryTemplate().execute(
                retryContext -> ccmV2Client.listAgents(requestId, accountId, environmentCrnOpt),
                retryExhausted -> {
                    LOGGER.error("Error Listing Agents for account '{}' and environmentCrn '{}', retryCount '{}'",
                            accountId, environmentCrnOpt, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error Listing CCM Agents", retryExhausted.getLastThrowable());
                });
    }

}

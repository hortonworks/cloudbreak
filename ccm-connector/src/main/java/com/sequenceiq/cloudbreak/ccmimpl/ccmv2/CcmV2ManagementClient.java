package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.config.GrpcCcmV2Config;

@Component
public class CcmV2ManagementClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmV2ManagementClient.class);

    @Inject
    private GrpcCcmV2Client grpcCcmV2Client;

    @Inject
    private GrpcCcmV2Config grpcCcmV2Config;

    public InvertingProxy awaitReadyInvertingProxyForAccount(String requestId, String accountId) {
        return getRetryTemplate().execute(
                retryContext -> {
                    LOGGER.debug("Retrieving InvertingProxy Config for accountId '{}'", accountId);
                    return Optional.ofNullable(grpcCcmV2Client.getOrCreateInvertingProxy(requestId, accountId, ThreadBasedUserCrnProvider.getUserCrn()))
                            .filter(invertingProxy -> InvertingProxy.Status.READY.equals(invertingProxy.getStatus()))
                            .orElseThrow(() -> new RuntimeException(String.format("InvertingProxy not found in ready state for accountId '%s'", accountId)));
                },
                retryExhausted -> {
                    LOGGER.error("Error retrieving InvertingProxy for accountId '{}', retryCount '{}'", accountId, retryExhausted.getRetryCount());
                    throw new RuntimeException("Error Retrieving CCM InvertingProxy Config", retryExhausted.getLastThrowable());
                });
    }

    public InvertingProxyAgent registerInvertingProxyAgent(String requestId, String accountId, String domainName, String keyId) {
        return getRetryTemplate().execute(
                retryContext -> {
                    LOGGER.debug("Registering Agent for accountId '{}', domainName '{}'", accountId, domainName);
                    return grpcCcmV2Client.registerAgent(requestId, accountId, domainName, keyId, ThreadBasedUserCrnProvider.getUserCrn());
                },
                retryExhausted -> {
                    LOGGER.error("Error Registering Agent for accountId '{}', domainName '{}', retryCount '{}'",
                            accountId, domainName, retryExhausted.getRetryCount());
                    throw new RuntimeException("Error Registering CCM Agent", retryExhausted.getLastThrowable());
                });
    }

    public UnregisterAgentResponse deregisterInvertingProxyAgent(String requestId, String agentCrn) {
        return getRetryTemplate().execute(
                retryContext -> {
                    LOGGER.debug("Deregistering Agent for agentCrn '{}'", agentCrn);
                    return grpcCcmV2Client.unRegisterAgent(requestId, agentCrn, ThreadBasedUserCrnProvider.getUserCrn());
                },
                retryExhausted -> {
                    LOGGER.error("Error Deregistering Agent for agentCrn '{}', retryCount '{}'", agentCrn, retryExhausted.getRetryCount());
                    throw new RuntimeException("Error Deregistering CCM Agent", retryExhausted.getLastThrowable());
                });
    }

    private RetryTemplate getRetryTemplate() {
        TimeoutRetryPolicy policy = new TimeoutRetryPolicy();
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();

        policy.setTimeout(grpcCcmV2Config.getTimeoutMs());
        fixedBackOffPolicy.setBackOffPeriod(grpcCcmV2Config.getPollingIntervalMs());

        RetryTemplate template = new RetryTemplate();
        template.setBackOffPolicy(fixedBackOffPolicy);
        template.setRetryPolicy(policy);
        template.setThrowLastExceptionOnExhausted(true);
        return template;
    }
}
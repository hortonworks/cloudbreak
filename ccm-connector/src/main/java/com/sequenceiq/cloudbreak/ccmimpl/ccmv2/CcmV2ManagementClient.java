package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.exception.CcmV2Exception;
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
                    LOGGER.debug("Retrieving InvertingProxy for accountId '{}'", accountId);
                    InvertingProxy invertingProxy = grpcCcmV2Client.getOrCreateInvertingProxy(requestId, accountId, ThreadBasedUserCrnProvider.getUserCrn());
                    LOGGER.debug("Retrieved InvertingProxy with status '{}' for accountId '{}'", invertingProxy.getStatus(), accountId);

                    if (!InvertingProxy.Status.READY.equals(invertingProxy.getStatus())) {
                        throw new CcmV2Exception(String.format("InvertingProxy is not available in 'READY' state for accountId '%s'", accountId));
                    }
                    return invertingProxy;
                },
                retryExhausted -> {
                    LOGGER.error("Error Retrieving InvertingProxy for accountId '{}', retryCount '{}'", accountId, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error Retrieving CCM InvertingProxy", retryExhausted.getLastThrowable());
                });
    }

    public InvertingProxyAgent registerInvertingProxyAgent(String requestId, String accountId, Optional<String> environmentCrnOpt,
        String domainName, String keyId) {
        return getRetryTemplate().execute(
                retryContext -> {
                    LOGGER.debug("Registering Agent for accountId '{}', environmentCrnOpt: '{}', domainName '{}'",
                            accountId, environmentCrnOpt, domainName);
                    InvertingProxyAgent invertingProxyAgent = grpcCcmV2Client.registerAgent(requestId, accountId, environmentCrnOpt,
                            domainName, keyId, ThreadBasedUserCrnProvider.getUserCrn());
                    LOGGER.debug("Registered Agent for accountId '{}', environmentCrnOpt: '{}', domainName '{}', invertingProxyAgent '{}'",
                            accountId, environmentCrnOpt, domainName, invertingProxyAgent);
                    return invertingProxyAgent;
                },
                retryExhausted -> {
                    LOGGER.error("Error Registering Agent for accountId '{}', domainName '{}', retryCount '{}'",
                            accountId, domainName, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error Registering CCM Agent", retryExhausted.getLastThrowable());
                });
    }

    public UnregisterAgentResponse deregisterInvertingProxyAgent(String requestId, String agentCrn) {
        return getRetryTemplate().execute(
                retryContext -> {
                    LOGGER.debug("Deregistering Agent for agentCrn '{}'", agentCrn);
                    UnregisterAgentResponse unregisterAgentResponse = grpcCcmV2Client.unRegisterAgent(requestId, agentCrn,
                            ThreadBasedUserCrnProvider.getUserCrn());
                    LOGGER.debug("Deregistered Agent for agentCrn '{}', unregisterAgentResponse '{}'", agentCrn, unregisterAgentResponse);
                    return unregisterAgentResponse;
                },
                retryExhausted -> {
                    LOGGER.error("Error Deregistering Agent for agentCrn '{}', retryCount '{}'", agentCrn, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error Deregistering CCM Agent", retryExhausted.getLastThrowable());
                });
    }

    public List<InvertingProxyAgent> listInvertingProxyAgents(String requestId, String accountId, Optional<String> environmentCrnOpt) {
        return getRetryTemplate().execute(
                retryContext -> {
                    LOGGER.debug("Listing Agents for account '{}' and environmentCrn '{}'", accountId, environmentCrnOpt);
                    List<InvertingProxyAgent> agentList = grpcCcmV2Client.listAgents(requestId, ThreadBasedUserCrnProvider.getUserCrn(),
                            accountId, environmentCrnOpt);
                    LOGGER.debug("Listed Agent for account '{}' and environmentCrn '{}' list count: '{}'", accountId, environmentCrnOpt, agentList.size());
                    return agentList;
                },
                retryExhausted -> {
                    LOGGER.error("Error Listing Agents for account '{}' and environmentCrn '{}', retryCount '{}'",
                            accountId, environmentCrnOpt, retryExhausted.getRetryCount());
                    throw new CcmV2Exception("Error Listing CCM Agents", retryExhausted.getLastThrowable());
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

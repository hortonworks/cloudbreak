package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy.Status;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.exception.CcmV2Exception;

@Component
class CcmV2Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmV2Client.class);

    @Inject
    private GrpcCcmV2Client grpcCcmV2Client;

    public InvertingProxy getOrCreateInvertingProxy(String accountId) {
        LOGGER.debug("Retrieving InvertingProxy for accountId '{}'", accountId);
        InvertingProxy invertingProxy = grpcCcmV2Client.getOrCreateInvertingProxy(accountId, ThreadBasedUserCrnProvider.getUserCrn());
        LOGGER.debug("Retrieved InvertingProxy with status '{}' for accountId '{}'", invertingProxy.getStatus(), accountId);

        if (!Status.READY.equals(invertingProxy.getStatus())) {
            throw new CcmV2Exception(String.format("InvertingProxy is not available in 'READY' state for accountId '%s'", accountId));
        }
        return invertingProxy;
    }

    public InvertingProxyAgent registerAgent(String accountId,
            Optional<String> environmentCrnOpt, String domainName, String keyId, Optional<String> hmacKeyOpt) {
        LOGGER.debug("Registering Agent for accountId '{}', environmentCrnOpt: '{}', domainName '{}'",
                accountId, environmentCrnOpt, domainName);
        InvertingProxyAgent invertingProxyAgent = grpcCcmV2Client.registerAgent(accountId, environmentCrnOpt, domainName, keyId,
                ThreadBasedUserCrnProvider.getUserCrn(), hmacKeyOpt);
        LOGGER.debug("Registered Agent for accountId '{}', environmentCrnOpt: '{}', domainName '{}', invertingProxyAgent '{}'",
                accountId, environmentCrnOpt, domainName, invertingProxyAgent);
        return invertingProxyAgent;
    }

    public UnregisterAgentResponse deregisterAgent(String agentCrn) {
        LOGGER.debug("Deregistering Agent for agentCrn '{}'", agentCrn);
        UnregisterAgentResponse unregisterAgentResponse = grpcCcmV2Client.unRegisterAgent(agentCrn, ThreadBasedUserCrnProvider.getUserCrn());
        LOGGER.debug("Deregistered Agent for agentCrn '{}', unregisterAgentResponse '{}'", agentCrn, unregisterAgentResponse);
        return unregisterAgentResponse;
    }

    public List<InvertingProxyAgent> listAgents(String accountId, Optional<String> environmentCrnOpt) {
        LOGGER.debug("Listing Agents for account '{}' and environmentCrn '{}'", accountId, environmentCrnOpt);
        List<InvertingProxyAgent> agentList = grpcCcmV2Client.listAgents(ThreadBasedUserCrnProvider.getUserCrn(), accountId, environmentCrnOpt);
        LOGGER.debug("Listed Agent for account '{}' and environmentCrn '{}' list count: '{}'", accountId, environmentCrnOpt, agentList.size());
        return agentList;
    }

    public InvertingProxyAgent createAgentAccessKeyPair(String accountId, String agentCrn) {
        LOGGER.debug("Create agent access key pair for agentCrn '{}'", agentCrn);
        InvertingProxyAgent invertingProxyAgent = grpcCcmV2Client.createAgentAccessKeyPair(ThreadBasedUserCrnProvider.getUserCrn(), accountId, agentCrn);
        LOGGER.debug("Created agent access key pair for agentCrn '{}'", agentCrn);
        return invertingProxyAgent;
    }

    public DeactivateAgentAccessKeyPairResponse deactivateAgentAccessKeyPair(String accountId, String accessKeyId) {
        LOGGER.debug("Deactivate agent access key pair for accessKeyId '{}'", accessKeyId);
        DeactivateAgentAccessKeyPairResponse deactivateResponse = grpcCcmV2Client.deactivateAgentAccessKeyPair(ThreadBasedUserCrnProvider.getUserCrn(),
                accountId, accessKeyId);
        LOGGER.debug("Deactivated agent access key pair for accessKeyId '{}'", accessKeyId);
        return deactivateResponse;
    }

}

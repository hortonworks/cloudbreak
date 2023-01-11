package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Grpc;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Grpc.ClusterConnectivityManagementV2BlockingStub;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.AES256Parameters;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest.Builder;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse;
import com.cloudera.thunderhead.service.common.paging.PagingProto.PageToken;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;

import io.grpc.ManagedChannel;

@Component
public class GrpcCcmV2Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcCcmV2Client.class);

    @Qualifier("ccmV2ManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    public InvertingProxy getOrCreateInvertingProxy(String requestId, String accountId, String actorCrn) {
        ClusterConnectivityManagementV2BlockingStub client = makeClient(channelWrapper.getChannel(), requestId, actorCrn);
        CreateOrGetInvertingProxyRequest invertingProxyRequest = CreateOrGetInvertingProxyRequest.newBuilder()
                .setAccountId(accountId)
                .build();

        LOGGER.debug("Calling getOrCreateInvertingProxy with params accountId: '{}'", accountId);
        CreateOrGetInvertingProxyResponse invertingProxyRequestResponse = client.createOrGetInvertingProxy(invertingProxyRequest);
        return invertingProxyRequestResponse.getInvertingProxy();
    }

    public InvertingProxyAgent registerAgent(String requestId, String accountId, Optional<String> environmentCrnOpt, String domainName,
            String keyId, String actorCrn, Optional<String> hmacKeyOpt) {
        ClusterConnectivityManagementV2BlockingStub client = makeClient(channelWrapper.getChannel(), requestId, actorCrn);
        RegisterAgentRequest.Builder registerAgentRequestBuilder = RegisterAgentRequest.newBuilder()
                .setAccountId(accountId)
                .setDomainName(domainName)
                .setKeyId(keyId);
        environmentCrnOpt.ifPresent(registerAgentRequestBuilder::setEnvironmentCrn);
        if (hmacKeyOpt.isPresent()) {
            AES256Parameters aes256Parameters = AES256Parameters.newBuilder().setHmacKey(hmacKeyOpt.get()).build();
            registerAgentRequestBuilder.setAes256Parameters(aes256Parameters);
        }
        RegisterAgentRequest registerAgentRequest = registerAgentRequestBuilder.build();
        LOGGER.debug("Calling registerAgent with params accountId: '{}', environmentCrnOpt: '{}', domainName: '{}', keyId:'{}' ",
                accountId, environmentCrnOpt, domainName, keyId);
        RegisterAgentResponse registerAgentResponse = client.registerAgent(registerAgentRequest);
        return registerAgentResponse.getInvertingProxyAgent();
    }

    public UnregisterAgentResponse unRegisterAgent(String requestId, String agentCrn, String actorCrn) {
        ClusterConnectivityManagementV2BlockingStub client = makeClient(channelWrapper.getChannel(), requestId, actorCrn);
        UnregisterAgentRequest unregisterAgentRequest = UnregisterAgentRequest.newBuilder()
                .setAgentCrn(agentCrn)
                .build();

        LOGGER.debug("Calling unRegisterAgent with params agentCrn: '{}'", agentCrn);
        return client.unregisterAgent(unregisterAgentRequest);
    }

    public List<InvertingProxyAgent> listAgents(String requestId, String actorCrn, String accountId, Optional<String> environmentCrnOpt) {
        List<InvertingProxyAgent> result = new ArrayList<>();
        ClusterConnectivityManagementV2BlockingStub client = makeClient(channelWrapper.getChannel(), requestId, actorCrn);
        Builder listAgentsRequestBuilder = ListAgentsRequest.newBuilder();
        environmentCrnOpt.ifPresentOrElse(listAgentsRequestBuilder::setEnvironmentCrn, () -> listAgentsRequestBuilder.setAccountId(accountId));
        ListAgentsRequest listAgentsRequest = listAgentsRequestBuilder.build();

        PageToken nextPageToken = null;
        int page = 0;
        while (page == 0 || nextPageToken.getExclusiveStartKeyStringAttrsCount() > 0 || nextPageToken.getExclusiveStartKeyNumAttrsCount() > 0) {
            ++page;
            LOGGER.debug("Calling listAgents with params accountId: '{}', environment CRN: '{}', page: '{}'", accountId, environmentCrnOpt, page);
            ListAgentsResponse listAgentsResponse = client.listAgents(listAgentsRequest);
            result.addAll(listAgentsResponse.getAgentsList());

            nextPageToken = listAgentsResponse.getNextPageToken();
            listAgentsRequestBuilder.setPageToken(nextPageToken);
            listAgentsRequest = listAgentsRequestBuilder.build();
        }
        return result;
    }

    private ClusterConnectivityManagementV2BlockingStub makeClient(ManagedChannel channel, String requestId, String actorCrn) {
        return ClusterConnectivityManagementV2Grpc.newBlockingStub(channel)
                .withInterceptors(new AltusMetadataInterceptor(requestId, actorCrn));
    }
}

package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Grpc;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Grpc.ClusterConnectivityManagementV2BlockingStub;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.config.GrpcCcmV2Config;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentracing.Tracer;

import java.util.Optional;

@Component
public class GrpcCcmV2Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcCcmV2Client.class);

    @Inject
    private GrpcCcmV2Config grpcCcmV2Config;

    @Inject
    private Tracer tracer;

    public InvertingProxy getOrCreateInvertingProxy(String requestId, String accountId, String actorCrn) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterConnectivityManagementV2BlockingStub client = makeClient(channelWrapper.getChannel(), requestId, actorCrn);
            CreateOrGetInvertingProxyRequest invertingProxyRequest = CreateOrGetInvertingProxyRequest.newBuilder()
                    .setAccountId(accountId)
                    .build();

            LOGGER.debug("Calling getOrCreateInvertingProxy with params accountId: '{}'", accountId);
            CreateOrGetInvertingProxyResponse invertingProxyRequestResponse = client.createOrGetInvertingProxy(invertingProxyRequest);
            return invertingProxyRequestResponse.getInvertingProxy();
        }
    }

    public InvertingProxyAgent registerAgent(String requestId, String accountId, Optional<String> environmentCrnOpt, String domainName,
        String keyId, String actorCrn) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterConnectivityManagementV2BlockingStub client = makeClient(channelWrapper.getChannel(), requestId, actorCrn);
            RegisterAgentRequest.Builder registerAgentRequestBuilder =  RegisterAgentRequest.newBuilder()
                    .setAccountId(accountId)
                    .setDomainName(domainName)
                    .setKeyId(keyId);
            environmentCrnOpt.ifPresent(environmentCrn -> registerAgentRequestBuilder.setEnvironmentCrn(environmentCrn));
            RegisterAgentRequest registerAgentRequest = registerAgentRequestBuilder.build();
            LOGGER.debug("Calling registerAgent with params accountId: '{}', environmentCrnOpt: '{}', domainName: '{}', keyId:'{}' ",
                    accountId, environmentCrnOpt, domainName, keyId);
            RegisterAgentResponse registerAgentResponse = client.registerAgent(registerAgentRequest);
            return registerAgentResponse.getInvertingProxyAgent();
        }
    }

    public UnregisterAgentResponse unRegisterAgent(String requestId, String agentCrn, String actorCrn) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterConnectivityManagementV2BlockingStub client = makeClient(channelWrapper.getChannel(), requestId, actorCrn);
            UnregisterAgentRequest unregisterAgentRequest =  UnregisterAgentRequest.newBuilder()
                    .setAgentCrn(agentCrn)
                    .build();

            LOGGER.debug("Calling unRegisterAgent with params agentCrn: '{}'", agentCrn);
            return client.unregisterAgent(unregisterAgentRequest);
        }
    }

    private ManagedChannelWrapper makeWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(grpcCcmV2Config.getHost(), grpcCcmV2Config.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }

    private ClusterConnectivityManagementV2BlockingStub makeClient(ManagedChannel channel, String requestId, String actorCrn) {
        return ClusterConnectivityManagementV2Grpc.newBlockingStub(channel)
                .withInterceptors(GrpcUtil.getTracingInterceptor(tracer),
                        new AltusMetadataInterceptor(requestId, actorCrn));
    }
}

package com.sequenceiq.cloudbreak.ccmimpl.cloudinit;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2Parameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2Parameters;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.CcmV2ManagementClient;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component
public class DefaultCcmV2ParameterSupplier implements CcmV2ParameterSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCcmV2ParameterSupplier.class);

    @Inject
    private CcmV2ManagementClient ccmV2Client;

    public CcmV2Parameters getCcmV2Parameters(@Nonnull String accountId, @Nonnull String clusterGatewayDomain, @Nonnull String agentKeyId) {
        String requestId = Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString())).orElse(UUID.randomUUID().toString());
        MDCBuilder.addRequestId(requestId);

        InvertingProxy invertingProxy = ccmV2Client.awaitReadyInvertingProxyForAccount(requestId, accountId);
        InvertingProxyAgent invertingProxyAgent = ccmV2Client.registerInvertingProxyAgent(requestId, accountId, clusterGatewayDomain, agentKeyId);

        return new DefaultCcmV2Parameters(invertingProxy.getHostname(), invertingProxy.getCertificate(),
                invertingProxyAgent.getAgentCrn(), agentKeyId,
                invertingProxyAgent.getEncipheredPrivateKey(), invertingProxyAgent.getCertificate(), clusterGatewayDomain);
    }
}
package com.sequenceiq.cloudbreak.ccmimpl.cloudinit;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2Parameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2Parameters;
import com.sequenceiq.cloudbreak.ccm.exception.CcmV2Exception;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.CcmV2ManagementClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component("DefaultCcmV2ParameterSupplier")
public class DefaultCcmV2ParameterSupplier implements CcmV2ParameterSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCcmV2ParameterSupplier.class);

    @Inject
    private CcmV2ManagementClient ccmV2Client;

    public CcmV2Parameters getCcmV2Parameters(@Nonnull String accountId, @Nonnull Optional<String> environmentCrnOpt,
        @Nonnull String clusterGatewayDomain, @Nonnull String agentKeyId) {

        InvertingProxyAndAgent invertingProxyAndAgent = getInvertingProxyAndAgent(accountId, environmentCrnOpt, clusterGatewayDomain, agentKeyId);
        InvertingProxy invertingProxy = invertingProxyAndAgent.getInvertingProxy();
        InvertingProxyAgent invertingProxyAgent = invertingProxyAndAgent.getInvertingProxyAgent();

        LOGGER.debug("CcmV2Config successfully retrieved InvertingProxyHost: '{}', InvertingProxyStatus: '{}', InvertingProxyAgentCrn: '{}', " +
                        "EnvironmentCrnOpt: '{}'", invertingProxy.getHostname(), invertingProxy.getStatus(),
                invertingProxyAgent.getAgentCrn(), Optional.of(invertingProxyAgent.getEnvironmentCrn()));

        return new DefaultCcmV2Parameters(invertingProxy.getHostname(), invertingProxy.getCertificate(), invertingProxyAgent.getAgentCrn(), agentKeyId,
                invertingProxyAgent.getEncipheredPrivateKey(), invertingProxyAgent.getCertificate());
    }

    protected InvertingProxyAndAgent getInvertingProxyAndAgent(@Nonnull String accountId, @Nonnull Optional<String> environmentCrnOpt,
            @Nonnull String clusterGatewayDomain, @Nonnull String agentKeyId) {
        String requestId = MDCBuilder.getOrGenerateRequestId();

        InvertingProxy invertingProxy = ccmV2Client.awaitReadyInvertingProxyForAccount(requestId, accountId);
        unregisterExistingAgent(requestId, accountId, environmentCrnOpt, agentKeyId);
        InvertingProxyAgent invertingProxyAgent = ccmV2Client.registerInvertingProxyAgent(requestId, accountId, environmentCrnOpt,
                clusterGatewayDomain, agentKeyId);
        validateCcmV2ConfigResponse(invertingProxy, invertingProxyAgent);
        return new InvertingProxyAndAgent(invertingProxy, invertingProxyAgent);
    }

    private void unregisterExistingAgent(String requestId, String accountId, Optional<String> environmentCrnOpt, String agentKeyId) {
        try {
            List<InvertingProxyAgent> invertingProxyAgents = ccmV2Client.listInvertingProxyAgents(requestId, accountId, environmentCrnOpt);
            Optional<InvertingProxyAgent> registeredAgentWithSameKey = invertingProxyAgents.stream()
                    .filter(agent -> agentKeyId.equals(CcmResourceUtil.getKeyId(agent.getAgentCrn())))
                    .findFirst();
            registeredAgentWithSameKey.ifPresent(agent -> ccmV2Client.deregisterInvertingProxyAgent(requestId, agent.getAgentCrn()));
        } catch (CcmV2Exception e) {
            throw new CcmV2Exception("Error in trying to deregister possibly existing CCM Agent", e);
        }
    }

    private void validateCcmV2ConfigResponse(InvertingProxy invertingProxy, InvertingProxyAgent invertingProxyAgent) {
        checkArgument(StringUtils.isNotEmpty(invertingProxy.getHostname()), "InvertingProxy Hostname is not initialized.");
        checkArgument(StringUtils.isNotEmpty(invertingProxy.getCertificate()), "InvertingProxy Certificate is not initialized.");
        checkArgument(StringUtils.isNotEmpty(invertingProxyAgent.getAgentCrn()), "InvertingProxyAgent Crn is not initialized.");
        if (StringUtils.isNotEmpty(invertingProxyAgent.getAccessKeyId())) {
            checkArgument(StringUtils.isNotEmpty(invertingProxyAgent.getEncipheredAccessKey()),
                    "InvertingProxyAgent Access Key ID is present but Enciphered Access Key is not initialized.");
        } else {
            checkArgument(StringUtils.isEmpty(invertingProxyAgent.getEncipheredAccessKey()),
                    "InvertingProxyAgent Access Key ID is not present but Enciphered Access Key is initialized. Error in inverting proxy logic.");
            checkArgument(StringUtils.isNotEmpty(invertingProxyAgent.getEncipheredPrivateKey()),
                    "InvertingProxyAgent Enciphered Private Key is not initialized.");
            checkArgument(StringUtils.isNotEmpty(invertingProxyAgent.getCertificate()), "InvertingProxyAgent Certificate is not initialized.");
        }
    }
}

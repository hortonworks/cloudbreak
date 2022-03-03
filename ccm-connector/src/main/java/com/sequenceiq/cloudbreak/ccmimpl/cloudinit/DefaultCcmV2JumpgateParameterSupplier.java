package com.sequenceiq.cloudbreak.ccmimpl.cloudinit;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2JumpgateParameters;

@Component("DefaultCcmV2JumpgateParameterSupplier")
public class DefaultCcmV2JumpgateParameterSupplier extends DefaultCcmV2ParameterSupplier implements CcmV2JumpgateParameterSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCcmV2JumpgateParameterSupplier.class);

    @Override
    public CcmV2JumpgateParameters getCcmV2JumpgateParameters(@Nonnull String accountId, @Nonnull Optional<String> environmentCrnOpt,
        @Nonnull String clusterGatewayDomain, @Nonnull String agentKeyId) {

        InvertingProxyAndAgent invertingProxyAndAgent = getInvertingProxyAndAgent(accountId, environmentCrnOpt, clusterGatewayDomain, agentKeyId);
        InvertingProxy invertingProxy = invertingProxyAndAgent.getInvertingProxy();
        InvertingProxyAgent invertingProxyAgent = invertingProxyAndAgent.getInvertingProxyAgent();

        LOGGER.debug("CcmV2JumpgateConfig successfully retrieved InvertingProxyHost: '{}', InvertingProxyStatus: '{}', InvertingProxyAgentCrn: '{}', " +
                        "EnvironmentCrnOpt: '{}'", invertingProxy.getHostname(), invertingProxy.getStatus(),
                invertingProxyAgent.getAgentCrn(), invertingProxyAgent.getEnvironmentCrn());

        return new DefaultCcmV2JumpgateParameters(invertingProxy.getHostname(), invertingProxy.getCertificate(), invertingProxyAgent.getAgentCrn(), agentKeyId,
                invertingProxyAgent.getEncipheredPrivateKey(), invertingProxyAgent.getCertificate(), invertingProxyAgent.getEnvironmentCrn(),
                invertingProxyAgent.getAccessKeyId(), invertingProxyAgent.getEncipheredAccessKey());
    }
}

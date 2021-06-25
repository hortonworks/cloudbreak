package com.sequenceiq.cloudbreak.ccmimpl.cloudinit;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2Parameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2Parameters;

@Component("DefaultCcmV2JumpgateParameterSupplier")
public class DefaultCcmV2JumpgateParameterSupplier extends DefaultCcmV2ParameterSupplier implements CcmV2JumpgateParameterSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCcmV2JumpgateParameterSupplier.class);

    public CcmV2JumpgateParameters getCcmV2JumpgateParameters(@Nonnull String accountId, @Nonnull Optional<String> environmentCrnOpt,
        @Nonnull String clusterGatewayDomain, @Nonnull String agentKeyId) {

        LOGGER.debug("Returning CCMV2 Jumpgate parameters");
        CcmV2Parameters ccmV2Parameters = getCcmV2Parameters(accountId, environmentCrnOpt, clusterGatewayDomain, agentKeyId);
        return new DefaultCcmV2Parameters(ccmV2Parameters.getInvertingProxyHost(), ccmV2Parameters.getInvertingProxyCertificate(),
                ccmV2Parameters.getAgentCrn(), agentKeyId, ccmV2Parameters.getAgentEncipheredPrivateKey(), ccmV2Parameters.getAgentCertificate());
    }

}

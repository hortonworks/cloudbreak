package com.sequenceiq.freeipa.service.image.userdata;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2Parameters;
import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class CcmUserDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmUserDataService.class);

    @Inject
    private CrnService crnService;

    @Inject
    private StackService stackService;

    @Inject
    private HostDiscoveryService hostDiscoveryService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private CcmParameterSupplier ccmParameterSupplier;

    @Inject
    @Qualifier("DefaultCcmV2ParameterSupplier")
    private CcmV2ParameterSupplier ccmV2ParameterSupplier;

    @Inject
    @Qualifier("DefaultCcmV2JumpgateParameterSupplier")
    private CcmV2JumpgateParameterSupplier ccmV2JumpgateParameterSupplier;

    public CcmConnectivityParameters fetchAndSaveCcmParameters(Stack stack) {
        CcmConnectivityParameters ccmConnectivityParameters = new CcmConnectivityParameters();
        String keyId = CcmResourceUtil.getKeyId(stack.getResourceCrn());

        if (stack.getTunnel().useCcmV1()) {
            ccmConnectivityParameters = getCcmConnectivityParameters(stack, keyId);
        } else if (stack.getTunnel().useCcmV2()) {
            ccmConnectivityParameters = getCcmV2ConnectivityParameters(stack, keyId);
        } else if (stack.getTunnel().useCcmV2Jumpgate()) {
            ccmConnectivityParameters = getCcmV2JumpgateConnectivityParameters(stack, keyId);
        } else {
            LOGGER.debug("CCM not enabled for stack.");
        }
        return ccmConnectivityParameters;
    }

    private CcmConnectivityParameters getCcmConnectivityParameters(Stack stack, String keyId) {
        CcmConnectivityParameters ccmConnectivityParameters;
        String actorCrn = Objects.requireNonNull(crnService.getUserCrn(), "userCrn is null");
        int gatewayPort = Optional.ofNullable(stack.getGatewayport()).orElse(ServiceFamilies.GATEWAY.getDefaultPort());
        Map<KnownServiceIdentifier, Integer> tunneledServicePorts = Collections.singletonMap(KnownServiceIdentifier.GATEWAY, gatewayPort);

        CcmParameters ccmV1Parameters = ccmParameterSupplier
                .getCcmParameters(actorCrn, stack.getAccountId(), keyId, tunneledServicePorts)
                .orElse(null);
        ccmConnectivityParameters = new CcmConnectivityParameters(ccmV1Parameters);
        saveCcmV1Config(stack.getId(), ccmV1Parameters);
        return ccmConnectivityParameters;
    }

    private void saveCcmV1Config(Long stackId, CcmParameters ccmV1Parameters) {
        if (null != ccmV1Parameters && null != ccmV1Parameters.getServerParameters()) {
            String minaSshdServiceId = ccmV1Parameters.getServerParameters().getMinaSshdServiceId();
            if (StringUtils.isNotBlank(minaSshdServiceId)) {
                LOGGER.debug("Adding Minasshdserviceid '{}' to stack", minaSshdServiceId);
                Stack stack = stackService.getStackById(stackId);
                stack.setMinaSshdServiceId(minaSshdServiceId);
                stackService.save(stack);
                LOGGER.debug("Added Minasshdserviceid '{}' to stack", minaSshdServiceId);
            }
        }
    }

    private CcmConnectivityParameters getCcmV2ConnectivityParameters(Stack stack, String keyId) {
        String generatedClusterDomain = getGatewayFqdn(stack);

        CcmV2Parameters ccmV2Parameters = ccmV2ParameterSupplier.getCcmV2Parameters(stack.getAccountId(), Optional.of(stack.getEnvironmentCrn()),
                generatedClusterDomain, keyId);
        CcmConnectivityParameters ccmConnectivityParameters = new CcmConnectivityParameters(ccmV2Parameters);
        saveCcmV2Config(stack.getId(), ccmV2Parameters);
        return ccmConnectivityParameters;
    }

    private String getGatewayFqdn(Stack stack) {
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        String gatewayHostName = hostDiscoveryService.generateHostname(freeIpa.getHostname(), null, 0, false);
        return hostDiscoveryService.determineGatewayFqdn(gatewayHostName, freeIpa.getDomain());
    }

    private void saveCcmV2Config(Long stackId, CcmV2Parameters ccmV2Parameters) {
        String ccmV2AgentCrn = ccmV2Parameters.getAgentCrn();
        if (StringUtils.isNotBlank(ccmV2AgentCrn)) {
            LOGGER.debug("Adding CcmV2AgentCrn '{}' to stack", ccmV2AgentCrn);
            Stack stack = stackService.getStackById(stackId);
            stack.setCcmV2AgentCrn(ccmV2AgentCrn);
            stackService.save(stack);
            LOGGER.debug("Added CcmV2AgentCrn  '{}' to stack", ccmV2AgentCrn);
        }
    }

    private CcmConnectivityParameters getCcmV2JumpgateConnectivityParameters(Stack stack, String keyId) {
        String generatedClusterDomain = getGatewayFqdn(stack);

        CcmV2JumpgateParameters ccmV2JumpgateParameters = ccmV2JumpgateParameterSupplier.getCcmV2JumpgateParameters(stack.getAccountId(),
                Optional.of(stack.getEnvironmentCrn()), generatedClusterDomain, keyId);
        CcmConnectivityParameters ccmConnectivityParameters = new CcmConnectivityParameters(ccmV2JumpgateParameters);
        saveCcmV2Config(stack.getId(), ccmV2JumpgateParameters);
        return ccmConnectivityParameters;
    }

}

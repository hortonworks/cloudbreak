package com.sequenceiq.cloudbreak.service.image.userdata;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2Parameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class CcmUserDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmUserDataService.class);

    @Inject
    private StackService stackService;

    @Inject
    private HostDiscoveryService hostDiscoveryService;

    @Inject
    private CcmParameterSupplier ccmParameterSupplier;

    @Inject
    @Qualifier("DefaultCcmV2ParameterSupplier")
    private CcmV2ParameterSupplier ccmV2ParameterSupplier;

    public CcmConnectivityParameters fetchAndSaveCcmParameters(Stack stack) {
        CcmConnectivityParameters ccmConnectivityParameters = new CcmConnectivityParameters();
        if (stack.getTunnel().useCcmV1()) {
            ccmConnectivityParameters = getCcmConnectivityParameters(stack);
        } else if (stack.getTunnel().useCcmV2()) {
            ccmConnectivityParameters = getCcmV2ConnectivityParameters(stack);
        } else if (stack.getTunnel().useCcmV2Jumpgate()) {
            ccmConnectivityParameters = getCcmV2JumpgateConnectivityParameters();
        } else {
            LOGGER.debug("CCM not enabled for stack.");
        }
        return ccmConnectivityParameters;
    }

    private CcmConnectivityParameters getCcmConnectivityParameters(Stack stack) {
        CcmConnectivityParameters ccmConnectivityParameters;
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String actorCrn = Objects.requireNonNull(userCrn, "userCrn is null");

        ImmutableMap.Builder<KnownServiceIdentifier, Integer> builder = ImmutableMap.builder();
        int gatewayPort = Optional.ofNullable(stack.getGatewayPort()).orElse(ServiceFamilies.GATEWAY.getDefaultPort());
        builder.put(KnownServiceIdentifier.GATEWAY, gatewayPort);

        // Optionally configure a tunnel for (nginx fronting) Knox
        if (stack.getCluster().getGateway() != null) {
            // JSA TODO Do we support a non-default port for the nginx that fronts Knox?
            builder.put(KnownServiceIdentifier.KNOX, ServiceFamilies.KNOX.getDefaultPort());
        }

        Map<KnownServiceIdentifier, Integer> tunneledServicePorts = builder.build();
        CcmParameters ccmV1Parameters = ccmParameterSupplier.getCcmParameters(actorCrn, ThreadBasedUserCrnProvider.getAccountId(),
                CcmResourceUtil.getKeyId(stack.getResourceCrn()), tunneledServicePorts).orElse(null);
        ccmConnectivityParameters = new CcmConnectivityParameters(ccmV1Parameters);

        saveCcmV1Config(stack.getId(), ccmV1Parameters);
        return ccmConnectivityParameters;
    }

    private void saveCcmV1Config(Long stackId, CcmParameters ccmV1Parameters) {
        if (null != ccmV1Parameters && null != ccmV1Parameters.getServerParameters()) {
            String minaSshdServiceId = ccmV1Parameters.getServerParameters().getMinaSshdServiceId();
            if (StringUtils.isNotBlank(minaSshdServiceId)) {
                LOGGER.debug("Adding Minasshdserviceid [{}] to stack", minaSshdServiceId);
                stackService.setMinaSshdServiceIdByStackId(stackId, minaSshdServiceId);
                LOGGER.debug("Added Minasshdserviceid [{}] to stack", minaSshdServiceId);
            }
        }
    }

    private CcmConnectivityParameters getCcmV2ConnectivityParameters(Stack stack) {
        String generatedGatewayFqdn = getGatewayFqdn(stack);

        CcmV2Parameters ccmV2Parameters = ccmV2ParameterSupplier.getCcmV2Parameters(ThreadBasedUserCrnProvider.getAccountId(),
                Optional.empty(), generatedGatewayFqdn, CcmResourceUtil.getKeyId(stack.getResourceCrn()));
        CcmConnectivityParameters ccmConnectivityParameters = new CcmConnectivityParameters(ccmV2Parameters);

        saveCcmV2Config(stack.getId(), ccmV2Parameters);
        return ccmConnectivityParameters;
    }

    private String getGatewayFqdn(Stack stack) {
        String gatewayHostName = hostDiscoveryService.generateHostname(stack.getCustomHostname(),
                stack.getGatewayHostGroup().map(InstanceGroup::getGroupName).orElse(""), 0L,
                stack.isClusterNameAsSubdomain());
        String stackDomain = hostDiscoveryService.determineDomain(stack.getCustomDomain(), stack.getName(), stack.isClusterNameAsSubdomain());
        return hostDiscoveryService.determineGatewayFqdn(gatewayHostName, stackDomain);
    }

    private void saveCcmV2Config(Long stackId, CcmV2Parameters ccmV2Parameters) {
        if (StringUtils.isNotBlank(ccmV2Parameters.getAgentCrn())) {
            LOGGER.debug("Adding CcmV2AgentCrn '{}' to stack", ccmV2Parameters.getAgentCrn());
            stackService.setCcmV2AgentCrnByStackId(stackId, ccmV2Parameters.getAgentCrn());
            LOGGER.debug("Added CcmV2AgentCrn  '{}' to stack", ccmV2Parameters.getAgentCrn());
        }
    }

    private CcmConnectivityParameters getCcmV2JumpgateConnectivityParameters() {
        CcmV2JumpgateParameters ccmV2JumpgateParameters = new DefaultCcmV2JumpgateParameters();
        return new CcmConnectivityParameters(ccmV2JumpgateParameters);
    }
}

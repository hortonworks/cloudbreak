package com.sequenceiq.freeipa.service.image.userdata;

import static com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V2_AGENT_ACCESS_KEY_ID;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V2_AGENT_ENCIPHERED_ACCESS_KEY;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V_2_AGENT_CERTIFICATE;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V_2_AGENT_ENCIPHERED_KEY;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V_2_AGENT_HMAC_FOR_PRIVATE_KEY;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V_2_AGENT_HMAC_KEY;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V_2_AGENT_KEY_ID;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V_2_INVERTING_PROXY_CERTIFICATE;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V_2_INVERTING_PROXY_HOST;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V_2_IV;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

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
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.util.UserDataReplacer;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
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
    private CcmV2TlsTypeDecider ccmV2TlsTypeDecider;

    @Inject
    private CachedEnvironmentClientService environmentService;

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
            ccmConnectivityParameters = getCcmV2JumpgateConnectivityParameters(stack, keyId, getHmacKeyOpt(stack));
        } else {
            LOGGER.debug("CCM not enabled for stack.");
        }
        return ccmConnectivityParameters;
    }

    public Optional<String> getHmacKeyOpt(Stack stack) {
        DetailedEnvironmentResponse environment = environmentService.getByCrn(stack.getEnvironmentCrn());
        Optional<String> hmacKeyOpt = CcmV2TlsType.ONE_WAY_TLS == ccmV2TlsTypeDecider.decide(environment)
                ? Optional.of(UUID.randomUUID().toString())
                : Optional.empty();
        return hmacKeyOpt;
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
        saveCcmV2Config(stack.getId(), ccmV2Parameters);
        return new CcmConnectivityParameters(ccmV2Parameters);
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
            stackService.setCcmV2AgentCrnByStackId(stackId, ccmV2AgentCrn);
            LOGGER.debug("Added CcmV2AgentCrn  '{}' to stack", ccmV2AgentCrn);
        }
    }

    private CcmConnectivityParameters getCcmV2JumpgateConnectivityParameters(Stack stack, String keyId, Optional<String> hmacKeyOpt) {
        String generatedClusterDomain = getGatewayFqdn(stack);

        CcmV2JumpgateParameters ccmV2JumpgateParameters = ccmV2JumpgateParameterSupplier.getCcmV2JumpgateParameters(stack.getAccountId(),
                Optional.of(stack.getEnvironmentCrn()), generatedClusterDomain, keyId, hmacKeyOpt);
        saveCcmV2Config(stack.getId(), ccmV2JumpgateParameters);
        saveCcmV2JumpgateParameters(stack.getId(), ccmV2JumpgateParameters);
        return new CcmConnectivityParameters(ccmV2JumpgateParameters);
    }

    private void saveCcmV2JumpgateParameters(Long stackId, CcmV2JumpgateParameters ccmV2JumpgateParameters) {
        LOGGER.debug("Adding CcmV2JumpgateParameters to stack");
        Stack stack = stackService.getStackById(stackId);
        stack.setCcmParameters(new CcmConnectivityParameters(ccmV2JumpgateParameters));
        stackService.save(stack);
    }

    public void saveOrUpdateStackCcmParameters(Stack stack, InvertingProxyAgent updatedInvertingProxyAgent, String modifiedUserData,
            Optional<String> hmacKey) {
        if (stack.getCcmParameters() != null && stack.getCcmParameters().getCcmV2JumpgateParameters() != null) {
            stack.setCcmParameters(updateCcmConnectivityParamsFromStack(stack, updatedInvertingProxyAgent, hmacKey));
        } else if (stack.getCcmParameters() == null || stack.getCcmParameters().getCcmV2JumpgateParameters() == null) {
            stack.setCcmParameters(createCcmConnectivityParametersFromUserData(stack, modifiedUserData));
        }
        stackService.save(stack);
    }

    private CcmConnectivityParameters updateCcmConnectivityParamsFromStack(Stack stack, InvertingProxyAgent updatedInvertingProxyAgent,
            Optional<String> hmacKey) {
        CcmV2JumpgateParameters ccmV2JumpgateParameters = stack.getCcmParameters().getCcmV2JumpgateParameters();
        DefaultCcmV2JumpgateParameters modifiedCcmV2JumpgateParameters = new DefaultCcmV2JumpgateParameters(ccmV2JumpgateParameters.getInvertingProxyHost(),
                ccmV2JumpgateParameters.getInvertingProxyCertificate(), stack.getCcmV2AgentCrn(), ccmV2JumpgateParameters.getAgentKeyId(),
                ccmV2JumpgateParameters.getAgentEncipheredPrivateKey(), ccmV2JumpgateParameters.getAgentCertificate(), stack.getEnvironmentCrn(),
                updatedInvertingProxyAgent.getAccessKeyId(), updatedInvertingProxyAgent.getEncipheredAccessKey(), hmacKey.orElse(EMPTY),
                updatedInvertingProxyAgent.getInitialisationVector(), updatedInvertingProxyAgent.getHmacForPrivateKey());
        return new CcmConnectivityParameters(modifiedCcmV2JumpgateParameters);
    }

    private CcmConnectivityParameters createCcmConnectivityParametersFromUserData(Stack stack, String modifiedUserData) {
        UserDataReplacer userDataReplacer = new UserDataReplacer(modifiedUserData);
        String invertingProxyHost = userDataReplacer.extractValueOrEmpty(CCM_V_2_INVERTING_PROXY_HOST);
        String invertingProxyCertificate = userDataReplacer.extractValueOrEmpty(CCM_V_2_INVERTING_PROXY_CERTIFICATE);
        String agentCrn = stack.getCcmV2AgentCrn();
        String agentKeyId = userDataReplacer.extractValueOrEmpty(CCM_V_2_AGENT_KEY_ID);
        String agentEncipheredPrivateKey = userDataReplacer.extractValueOrEmpty(CCM_V_2_AGENT_ENCIPHERED_KEY);
        String agentCertificate = userDataReplacer.extractValueOrEmpty(CCM_V_2_AGENT_CERTIFICATE);
        String environmentCrn = stack.getEnvironmentCrn();
        String agentMachineUserAccessKey = userDataReplacer.extractValueOrEmpty(CCM_V2_AGENT_ACCESS_KEY_ID);
        String agentMachineUserEncipheredAccessKey = userDataReplacer.extractValueOrEmpty(CCM_V2_AGENT_ENCIPHERED_ACCESS_KEY);
        String hmacKey = userDataReplacer.extractValueOrEmpty(CCM_V_2_AGENT_HMAC_KEY);
        String initialisationVector = userDataReplacer.extractValueOrEmpty(CCM_V_2_IV);
        String hmacForPrivateKey = userDataReplacer.extractValueOrEmpty(CCM_V_2_AGENT_HMAC_FOR_PRIVATE_KEY);
        DefaultCcmV2JumpgateParameters ccmV2JumpgateParameters = new DefaultCcmV2JumpgateParameters(invertingProxyHost, invertingProxyCertificate, agentCrn,
                agentKeyId, agentEncipheredPrivateKey, agentCertificate, environmentCrn, agentMachineUserAccessKey, agentMachineUserEncipheredAccessKey,
                hmacKey, initialisationVector, hmacForPrivateKey);
        return new CcmConnectivityParameters(ccmV2JumpgateParameters);
    }
}

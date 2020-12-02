package com.sequenceiq.freeipa.service.image.userdata;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_AGENT_CRN;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_CLUSTER_DOMAIN;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityMode;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2Parameters;
import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.service.GetCloudParameterException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.cloud.PlatformParameterService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class UserDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataService.class);

    private static final int CCM_KEY_ID_LENGTH = 36;

    @Inject
    private UserDataBuilder userDataBuilder;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Autowired
    private CcmParameterSupplier ccmParameterSupplier;

    @Autowired
    private CcmV2ParameterSupplier ccmV2ParameterSupplier;

    @Inject
    private CrnService crnService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private StackService stackService;

    @Inject
    private ImageService imageService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private HostDiscoveryService hostDiscoveryService;

    @Inject
    private FreeIpaService freeIpaService;

    public void createUserData(Long stackId) {
        Stack stack = stackService.getStackById(stackId);
        Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn());
        Optional<String> requestId = MDCUtils.getRequestId();
        Future<PlatformParameters> platformParametersFuture =
                intermediateBuilderExecutor.submit(() -> platformParameterService.getPlatformParameters(requestId, stack, credential));
        SecurityConfig securityConfig = stack.getSecurityConfig();
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        String cbPrivKey = saltSecurityConfig.getSaltBootSignPrivateKey();
        byte[] cbSshKeyDer = PkiUtil.getPublicKeyDer(new String(Base64.decodeBase64(cbPrivKey)));
        String sshUser = stack.getStackAuthentication().getLoginUserName();
        String cbCert = securityConfig.getClientCert();
        String saltBootPassword = saltSecurityConfig.getSaltBootPassword();
        try {
            PlatformParameters platformParameters = platformParametersFuture.get();
            CcmConnectivityParameters ccmParameters = fetchCcmParameters(stack);
            Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByEnvironmentCrn(stack.getEnvironmentCrn());
            String userData = userDataBuilder.buildUserData(Platform.platform(stack.getCloudPlatform()), cbSshKeyDer, sshUser, platformParameters,
                    saltBootPassword, cbCert, ccmParameters, proxyConfig.orElse(null));
            imageService.decorateImageWithUserDataForStack(stack, userData);
            saveStackCCMParameters(stack, ccmParameters);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Failed to get Platform parmaters", e);
            throw new GetCloudParameterException("Failed to get Platform parmaters", e);
        }
    }

    @VisibleForTesting
    CcmConnectivityParameters fetchCcmParameters(Stack stack) {
        CcmConnectivityParameters ccmConnectivityParameters = new CcmConnectivityParameters(CcmConnectivityMode.NONE);
        if (stack.getTunnel().useCcm()) {
            int gatewayPort = Optional.ofNullable(stack.getGatewayport()).orElse(ServiceFamilies.GATEWAY.getDefaultPort());
            Map<KnownServiceIdentifier, Integer> tunneledServicePorts = Collections.singletonMap(KnownServiceIdentifier.GATEWAY, gatewayPort);
            String keyId = CcmResourceUtil.getKeyId(stack.getResourceCrn());
            String actorCrn = Objects.requireNonNull(crnService.getUserCrn(), "userCrn is null");

            if (!entitlementService.ccmV2Enabled(INTERNAL_ACTOR_CRN, stack.getAccountId())) {
                CcmParameters ccmV1Parameters = ccmParameterSupplier
                        .getCcmParameters(actorCrn, stack.getAccountId(), keyId, tunneledServicePorts)
                        .orElse(null);
                ccmConnectivityParameters = new CcmConnectivityParameters(ccmV1Parameters);
            } else {
                FreeIpa freeIpa = freeIpaService.findByStack(stack);
                String gatewayHostName = hostDiscoveryService.generateHostname(freeIpa.getHostname(), null, 0, false);
                String generatedClusterDomain = hostDiscoveryService.determineDefaultDomainForStack(gatewayHostName, freeIpa.getDomain());

                CcmV2Parameters ccmV2Parameters = ccmV2ParameterSupplier.getCcmV2Parameters(stack.getAccountId(), generatedClusterDomain, keyId);
                ccmConnectivityParameters = new CcmConnectivityParameters(ccmV2Parameters);
            }
        }
        return ccmConnectivityParameters;
    }

    @VisibleForTesting
    void saveStackCCMParameters(Stack stack, CcmConnectivityParameters ccmConnectivityParameters) {
        if (CcmConnectivityMode.CCMV1.equals(ccmConnectivityParameters.getConnectivityMode())
                && ccmConnectivityParameters.getCcmParameters() != null) {
            String minaSshdServiceId = ccmConnectivityParameters.getCcmParameters().getServerParameters().getMinaSshdServiceId();
            if (StringUtils.isNotBlank(minaSshdServiceId)) {
                LOGGER.debug("Add Minasshdserviceid [{}] to stack [{}]", minaSshdServiceId, stack.getResourceCrn());
                stack.setMinaSshdServiceId(minaSshdServiceId);
                stackService.save(stack);
            }
        } else if (CcmConnectivityMode.CCMV2.equals(ccmConnectivityParameters.getConnectivityMode())
                && ccmConnectivityParameters.getCcmV2Parameters() != null) {
            CcmV2Parameters ccmV2Parameters = ccmConnectivityParameters.getCcmV2Parameters();
            Map<String, String> agentConfig = Map.of(CCMV2_AGENT_CRN, ccmV2Parameters.getAgentCrn(),
                    CCMV2_CLUSTER_DOMAIN, ccmV2Parameters.getClusterGatewayDomain());
            stack.setCcmV2Configs(Json.silent(agentConfig));
            stackService.save(stack);
        } else {
            LOGGER.debug("CCM not configured for stack '{}'", stack.getResourceCrn());
        }
    }
}

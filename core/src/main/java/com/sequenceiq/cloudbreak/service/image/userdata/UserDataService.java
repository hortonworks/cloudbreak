package com.sequenceiq.cloudbreak.service.image.userdata;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_AGENT_CRN;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_CLUSTER_DOMAIN;

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
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
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
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Service
public class UserDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataService.class);

    private static final int CCM_KEY_ID_LENGTH = 36;

    @Inject
    private UserDataBuilder userDataBuilder;

    @Inject
    private ServiceProviderConnectorAdapter connector;

    @Inject
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Autowired
    private CcmParameterSupplier ccmParameterSupplier;

    @Autowired
    private CcmV2ParameterSupplier ccmV2ParameterSupplier;

    @Inject
    private StackService stackService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ImageService imageService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private HostDiscoveryService hostDiscoveryService;

    public void createUserData(Long stackId) throws CloudbreakImageNotFoundException {
        Stack stack = stackService.getByIdWithLists(stackId);
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Future<PlatformParameters> platformParametersFuture =
                intermediateBuilderExecutor.submit(() -> connector.getPlatformParameters(stack, userCrn));

        SecurityConfig securityConfig = securityConfigService.generateAndSaveSecurityConfig(stack);
        stack.setSecurityConfig(securityConfig);
        stackService.save(stack);

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
            Map<InstanceGroupType, String> userData = userDataBuilder.buildUserData(Platform.platform(stack.getCloudPlatform()), cbSshKeyDer,
                    sshUser, platformParameters, saltBootPassword, cbCert, ccmParameters, proxyConfig.orElse(null));
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
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
            String keyId = CcmResourceUtil.getKeyId(stack.getResourceCrn());
            String actorCrn = Objects.requireNonNull(userCrn, "userCrn is null");

            if (!entitlementService.ccmV2Enabled(INTERNAL_ACTOR_CRN, accountId)) {
                ImmutableMap.Builder<KnownServiceIdentifier, Integer> builder = ImmutableMap.builder();
                int gatewayPort = Optional.ofNullable(stack.getGatewayPort()).orElse(ServiceFamilies.GATEWAY.getDefaultPort());
                builder.put(KnownServiceIdentifier.GATEWAY, gatewayPort);

                // Optionally configure a tunnel for (nginx fronting) Knox
                if (stack.getCluster().getGateway() != null) {
                    // JSA TODO Do we support a non-default port for the nginx that fronts Knox?
                    builder.put(KnownServiceIdentifier.KNOX, ServiceFamilies.KNOX.getDefaultPort());
                }

                Map<KnownServiceIdentifier, Integer> tunneledServicePorts = builder.build();
                CcmParameters ccmV1Parameters = ccmParameterSupplier.getCcmParameters(actorCrn, accountId, keyId, tunneledServicePorts).orElse(null);
                ccmConnectivityParameters = new CcmConnectivityParameters(ccmV1Parameters);
            } else {
                String gatewayHostName = hostDiscoveryService.generateHostname(stack.getCustomHostname(),
                        stack.getGatewayHostGroup().map(InstanceGroup::getGroupName).orElse(""), 0L,
                        stack.isClusterNameAsSubdomain());
                String stackDomain = hostDiscoveryService.determineDomain(stack.getCustomDomain(), stack.getName(), stack.isClusterNameAsSubdomain());
                String generatedClusterDomain = hostDiscoveryService.determineDefaultDomainForStack(gatewayHostName, stackDomain);

                CcmV2Parameters ccmV2Parameters = ccmV2ParameterSupplier.getCcmV2Parameters(accountId, generatedClusterDomain, keyId);
                ccmConnectivityParameters = new CcmConnectivityParameters(ccmV2Parameters);
            }
        }
        return ccmConnectivityParameters;
    }

    @VisibleForTesting
    void saveStackCCMParameters(Stack stack, CcmConnectivityParameters ccmConnectivityParameters) {
        long stackId = stack.getId();
        if (CcmConnectivityMode.CCMV1.equals(ccmConnectivityParameters.getConnectivityMode())
                && ccmConnectivityParameters.getCcmParameters() != null) {
            String minaSshdServiceId = ccmConnectivityParameters.getCcmParameters().getServerParameters().getMinaSshdServiceId();
            if (StringUtils.isNotBlank(minaSshdServiceId)) {
                LOGGER.debug("Add Minasshdserviceid [{}] to stack [{}]", minaSshdServiceId, stack.getResourceCrn());
                stackService.setMinaSshdServiceIdByStackId(stackId, minaSshdServiceId);
            }
        } else if (CcmConnectivityMode.CCMV2.equals(ccmConnectivityParameters.getConnectivityMode())
                && ccmConnectivityParameters.getCcmV2Parameters() != null) {
            CcmV2Parameters ccmV2Parameters = ccmConnectivityParameters.getCcmV2Parameters();
            Map<String, String> agentConfig = Map.of(CCMV2_AGENT_CRN, ccmV2Parameters.getAgentCrn(),
                    CCMV2_CLUSTER_DOMAIN, ccmV2Parameters.getClusterGatewayDomain());
            stackService.setCcmV2ConfigsByStackId(stackId, Json.silent(agentConfig));
        } else {
            LOGGER.debug("CCM not configured for stack '{}'", stack.getResourceCrn());
        }
    }
}
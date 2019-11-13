package com.sequenceiq.cloudbreak.service.image.userdata;

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

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameters;
import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.service.GetCloudParameterException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageService;
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

    @Autowired(required = false)
    private CcmParameterSupplier ccmParameterSupplier;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private StackService stackService;

    @Inject
    private ImageService imageService;

    public void createUserData(Long stackId) throws CloudbreakImageNotFoundException {
        Stack stack = stackService.getById(stackId);
        Future<PlatformParameters> platformParametersFuture =
                intermediateBuilderExecutor.submit(() -> connector.getPlatformParameters(stack));
        SecurityConfig securityConfig = stack.getSecurityConfig();
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        String cbPrivKey = saltSecurityConfig.getSaltBootSignPrivateKey();
        byte[] cbSshKeyDer = PkiUtil.getPublicKeyDer(new String(Base64.decodeBase64(cbPrivKey)));
        String sshUser = stack.getStackAuthentication().getLoginUserName();
        String cbCert = securityConfig.getClientCert();
        String saltBootPassword = saltSecurityConfig.getSaltBootPassword();
        try {
            PlatformParameters platformParameters = platformParametersFuture.get();
            CcmParameters ccmParameters = fetchCcmParameters(stack);
            Map<InstanceGroupType, String> userData = userDataBuilder.buildUserData(Platform.platform(stack.getCloudPlatform()), cbSshKeyDer,
                    sshUser, platformParameters, saltBootPassword, cbCert, ccmParameters);
            imageService.decorateImageWithUserDataForStack(stack, userData);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Failed to get Platform parmaters", e);
            throw new GetCloudParameterException("Failed to get Platform parmaters", e);
        }
    }

    private CcmParameters fetchCcmParameters(Stack stack) {
        CcmParameters ccmParameters = null;
        if ((ccmParameterSupplier != null) && stack.getUseCcm()) {
            ImmutableMap.Builder<KnownServiceIdentifier, Integer> builder = ImmutableMap.builder();
            int gatewayPort = Optional.ofNullable(stack.getGatewayPort()).orElse(ServiceFamilies.GATEWAY.getDefaultPort());
            builder.put(KnownServiceIdentifier.GATEWAY, gatewayPort);

            // Optionally configure a tunnel for (nginx fronting) Knox
            if (stack.getCluster().getGateway() != null) {
                // JSA TODO Do we support a non-default port for the nginx that fronts Knox?
                builder.put(KnownServiceIdentifier.KNOX, ServiceFamilies.KNOX.getDefaultPort());
            }

            Map<KnownServiceIdentifier, Integer> tunneledServicePorts = builder.build();

            String accountId = threadBasedUserCrnProvider.getAccountId();
            String userCrn = threadBasedUserCrnProvider.getUserCrn();
            // JSA TODO Use stack ID or something else instead?
            String keyId = StringUtils.right(stack.getResourceCrn(), CCM_KEY_ID_LENGTH);
            String actorCrn = Objects.requireNonNull(userCrn, "userCrn is null");
            ccmParameters = ccmParameterSupplier.getCcmParameters(actorCrn, accountId, keyId, tunneledServicePorts).orElse(null);
        }
        return ccmParameters;
    }
}

package com.sequenceiq.freeipa.service.image.userdata;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.service.GetCloudParameterException;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.cloud.PlatformParameterService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class UserDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataService.class);

    @Inject
    private UserDataBuilder userDataBuilder;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private CredentialService credentialService;

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private StackService stackService;

    @Inject
    private ImageService imageService;

    @Inject
    private CcmUserDataService ccmUserDataService;

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
            CcmConnectivityParameters ccmParameters = ccmUserDataService.fetchAndSaveCcmParameters(stack);
            Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByEnvironmentCrn(stack.getEnvironmentCrn());
            String userData = userDataBuilder.buildUserData(Platform.platform(stack.getCloudPlatform()), cbSshKeyDer, sshUser, platformParameters,
                    saltBootPassword, cbCert, ccmParameters, proxyConfig.orElse(null));
            imageService.decorateImageWithUserDataForStack(stack, userData);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Failed to get Platform parmaters", e);
            throw new GetCloudParameterException("Failed to get Platform parmaters", e);
        }
    }
}

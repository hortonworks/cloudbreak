package com.sequenceiq.freeipa.service.image.userdata;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.inject.Inject;

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
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigUserDataReplacer;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.util.UserDataReplacer;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
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
    private SecretService secretService;

    @Inject
    private CcmUserDataService ccmUserDataService;

    @Inject
    private CachedEnvironmentClientService environmentClientService;

    @Inject
    private ProxyConfigUserDataReplacer proxyConfigUserDataReplacer;

    public void createUserData(Long stackId) {
        Stack stack = getStack(stackId);
        LOGGER.debug("Creating user data for stack {}", stack.getResourceName());
        createUserData(stack, () -> ccmUserDataService.fetchAndSaveCcmParameters(stack));
    }

    public void regenerateUserDataForCcmUpgrade(Long stackId) {
        Stack stack = getStack(stackId);
        LOGGER.debug("Regenerating user data for stack {}", stack.getResourceName());
        createUserData(stack, stack::getCcmParameters);
    }

    public void updateJumpgateFlagOnly(Long stackId) {
        LOGGER.debug("Updating Jumpgate flag in user data for stack {}", stackId);
        ImageEntity image = imageService.getByStackId(stackId);
        String userData = new UserDataReplacer(image.getUserdataWrapper())
                .replace("IS_CCM_ENABLED", false)
                .replace("IS_CCM_V2_ENABLED", true)
                .replace("IS_CCM_V2_JUMPGATE_ENABLED", true)
                .getUserData();
        createOrUpdateUserData(stackId, userData);
    }

    public void updateProxyConfig(Long stackId) {
        LOGGER.debug("Updating proxy config in user data for stack {}", stackId);
        Stack stack = getStack(stackId);
        ImageEntity image = imageService.getByStackId(stackId);
        String userData = proxyConfigUserDataReplacer.replaceProxyConfigInUserDataByEnvCrn(image.getUserdataWrapper(), stack.getEnvironmentCrn());
        createOrUpdateUserData(stackId, userData);
    }

    public ImageEntity createOrUpdateUserData(Long stackId, String userdata) {
        LOGGER.debug("Updating user data for stack {}", stackId);
        ImageEntity image = imageService.getByStackId(stackId);

        String gatewayUserdataSecret = image.getGatewayUserdataSecret().getSecret();

        image.setUserdata(null);
        image.setGatewayUserdata(userdata);

        secretService.deleteByVaultSecretJson(gatewayUserdataSecret);

        return imageService.save(image);
    }

    public ImageEntity updateUserData(Long stackId, Function<String, String> userdataUpdateFunction) {
        LOGGER.debug("Updating user data for stack {}", stackId);
        ImageEntity image = imageService.getByStackId(stackId);

        String gatewayUserdataSecret = image.getGatewayUserdataSecret().getSecret();
        String userdata = userdataUpdateFunction.apply(image.getGatewayUserdata());

        image.setUserdata(null);
        image.setGatewayUserdata(userdata);

        secretService.deleteByVaultSecretJson(gatewayUserdataSecret);

        return imageService.save(image);
    }

    private void createUserData(Stack stack, Supplier<CcmConnectivityParameters> ccmParametersSupplier) {
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
        Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn());
        Future<PlatformParameters> platformParametersFuture =
                intermediateBuilderExecutor.submit(() -> platformParameterService.getPlatformParameters(stack, credential));
        SecurityConfig securityConfig = stack.getSecurityConfig();
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        String cbPrivKey = saltSecurityConfig.getSaltBootSignPrivateKey();
        byte[] cbSshKeyDer = PkiUtil.getPublicKeyDer(new String(Base64.decodeBase64(cbPrivKey)));
        String sshUser = stack.getStackAuthentication().getLoginUserName();
        String cbCert = securityConfig.getClientCert();
        String saltBootPassword = saltSecurityConfig.getSaltBootPassword();
        try {
            PlatformParameters platformParameters = platformParametersFuture.get();
            CcmConnectivityParameters ccmParameters = ccmParametersSupplier.get();
            Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByEnvironmentCrn(stack.getEnvironmentCrn());
            String userData = userDataBuilder.buildUserData(stack, environment, Platform.platform(stack.getCloudPlatform()),
                    cbSshKeyDer, sshUser, platformParameters, saltBootPassword, cbCert, ccmParameters, proxyConfig.orElse(null));
            createOrUpdateUserData(stack.getId(), userData);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Failed to get Platform parameters", e);
            throw new GetCloudParameterException("Failed to get Platform parameters", e);
        }
    }

    private Stack getStack(Long stackId) {
        Stack stack = stackService.getStackById(stackId);
        MDCBuilder.buildMdcContext(stack);
        return stack;
    }
}

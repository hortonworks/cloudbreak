package com.sequenceiq.cloudbreak.service.image.userdata;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.service.GetCloudParameterException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.Userdata;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.repository.UserdataRepository;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigUserDataReplacer;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.util.UserDataReplacer;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Service
public class UserDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataService.class);

    @Inject
    private UserdataRepository userdataRepository;

    @Inject
    private UserDataBuilder userDataBuilder;

    @Inject
    private ServiceProviderConnectorAdapter connector;

    @Inject
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private StackService stackService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ImageService imageService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private SecretService secretService;

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private CcmUserDataService ccmUserDataService;

    @Inject
    private ProxyConfigUserDataReplacer proxyConfigUserDataReplacer;

    public void updateJumpgateFlagOnly(Long stackId) {
        LOGGER.debug("Updating Jumpgate flag in user data for stack {}", stackId);
        Map<InstanceGroupType, String> userdata = getUserData(stackId);
        String gatewayUserdata = userdata.get(InstanceGroupType.GATEWAY);
        String result = new UserDataReplacer(gatewayUserdata)
                .replace("IS_CCM_V2_JUMPGATE_ENABLED", true)
                .replace("IS_CCM_V2_ENABLED",  true)
                .replace("IS_CCM_ENABLED",  false)
                .getUserData();
        userdata.put(InstanceGroupType.GATEWAY, result);
        updateUserData(stackId, userdata);
    }

    public void updateProxyConfig(Long stackId) {
        LOGGER.debug("Updating proxy config in user data for stack {}", stackId);
        Stack stack = stackService.getByIdWithLists(stackId);
        Map<InstanceGroupType, String> userDataByInstanceGroup = getUserData(stackId);
        String gatewayUserData = userDataByInstanceGroup.get(InstanceGroupType.GATEWAY);
        String result = proxyConfigUserDataReplacer.replaceProxyConfigInUserDataByEnvCrn(gatewayUserData, stack.getEnvironmentCrn());
        userDataByInstanceGroup.put(InstanceGroupType.GATEWAY, result);
        updateUserData(stack.getId(), userDataByInstanceGroup);
    }

    public Map<InstanceGroupType, String> getUserData(Long stackId) {
        try {
            Image image = imageService.getImage(stackId);
            if (image.getUserdata() == null || image.getUserdata().isEmpty()) {
                Map<InstanceGroupType, String> map = new HashMap<>();
                Optional<Userdata> userdataOptional = userdataRepository.findByStackId(stackId);
                if (userdataOptional.isPresent()) {
                    map.put(InstanceGroupType.CORE, userdataOptional.get().getCoreUserdata());
                    map.put(InstanceGroupType.GATEWAY, userdataOptional.get().getGatewayUserdata());
                }
                return map;
            } else {
                return new HashMap<>(image.getUserdata());
            }
        } catch (CloudbreakImageNotFoundException e) {
            throw convertToServiceException(e);
        }
    }

    public Userdata updateUserData(Long stackId, Map<InstanceGroupType, String> userdata) {
        Stack stack = stackService.get(stackId);
        Userdata result;

        Optional<Userdata> userdataOptional = userdataRepository.findByStackId(stackId);

        if (userdataOptional.isPresent()) {
            Userdata existingUserData = userdataOptional.get();
            String coreUserdataSecret = existingUserData.getCoreUserdataSecret();
            String gatewayUserdataSecret = existingUserData.getGatewayUserdataSecret();

            existingUserData.setCoreUserdata(userdata.get(InstanceGroupType.CORE));
            existingUserData.setGatewayUserdata(userdata.get(InstanceGroupType.GATEWAY));
            result = userdataRepository.save(existingUserData);

            secretService.delete(coreUserdataSecret);
            secretService.delete(gatewayUserdataSecret);
        } else {
            Userdata newUserdata = new Userdata();
            newUserdata.setAccountId(stack.getWorkspace().getTenant().getName());
            newUserdata.setCoreUserdata(userdata.get(InstanceGroupType.CORE));
            newUserdata.setGatewayUserdata(userdata.get(InstanceGroupType.GATEWAY));
            newUserdata.setStack(stack);
            result = userdataRepository.save(newUserdata);
        }
        return result;
    }

    private static CloudbreakServiceException convertToServiceException(CloudbreakImageNotFoundException e) {
        String message = "Image not found for user data update";
        LOGGER.error(message);
        return new CloudbreakServiceException(message, e);
    }

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
            CcmConnectivityParameters ccmParameters = ccmUserDataService.fetchAndSaveCcmParameters(stack);
            Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByEnvironmentCrn(stack.getEnvironmentCrn());
            Map<InstanceGroupType, String> userData = userDataBuilder.buildUserData(Platform.platform(stack.getCloudPlatform()), cbSshKeyDer,
                    sshUser, platformParameters, saltBootPassword, cbCert, ccmParameters, proxyConfig.orElse(null));
            updateUserData(stackId, userData);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Failed to get Platform parmaters", e);
            throw new GetCloudParameterException("Failed to get Platform parmaters", e);
        }
    }
}

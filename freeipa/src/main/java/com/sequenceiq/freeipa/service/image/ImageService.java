package com.sequenceiq.freeipa.service.image;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.client.PkiUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.freeipa.entity.Image;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.ImageRepository;
import com.sequenceiq.freeipa.service.cloud.PlatformParameterService;

@Service
public class ImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);

    @Inject
    private UserDataBuilder userDataBuilder;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private ExecutorService executorService;

    @Inject
    private ImageRepository imageRepository;

    public void create(Stack stack, ImageSettingsV4Request imageRequest) {
        Future<PlatformParameters> platformParametersFuture = executorService.submit(() -> platformParameterService.getPlatformParameters(stack));
        String userData = createUserData(stack, platformParametersFuture);
        Image image = new Image();
        image.setStack(stack);
        image.setUserdata(userData);
        //FIXME remove hardcoded image name
        image.setImageName("ami-0a21cab39fdfb6b1a");
        imageRepository.save(image);
    }

    private String createUserData(Stack stack, Future<PlatformParameters> platformParametersFuture) {
        SecurityConfig securityConfig = stack.getSecurityConfig();
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        String cbPrivKey = saltSecurityConfig.getSaltBootSignPrivateKey();
        byte[] cbSshKeyDer = PkiUtil.getPublicKeyDer(new String(Base64.decodeBase64(cbPrivKey)));
        String sshUser = stack.getStackAuthentication().getLoginUserName();
        String cbCert = securityConfig.getClientCert();
        String saltBootPassword = saltSecurityConfig.getSaltBootPassword();
        PlatformParameters platformParameters = null;
        try {
            platformParameters = platformParametersFuture.get();
            return userDataBuilder.buildUserData(Platform.platform(stack.getCloudPlatform()), cbSshKeyDer, sshUser, platformParameters, saltBootPassword,
                    cbCert);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Failed to get Platform parmaters", e);
            throw new RuntimeException("Failed to get Platform parmaters", e);
        }
    }

    public Image getByStack(Stack stack) {
        return imageRepository.getByStack(stack);
    }
}

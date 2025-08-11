package com.sequenceiq.cloudbreak.service.salt;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_MASTER_KEY_PAIR;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_SIGN_KEY_PAIR;
import static org.apache.commons.codec.binary.Base64.decodeBase64;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.event.EventSelectorUtil;

@Service
public class SaltVersionUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltVersionUpgradeService.class);

    @Inject
    private ImageService imageService;

    @Inject
    private StackDtoService stackDtoService;

    public void validateSaltVersion(StackDto stack) {
        Set<String> gatewayInstancesWithOutdatedSaltVersion = getGatewayInstancesWithOutdatedSaltVersion(stack);
        if (!gatewayInstancesWithOutdatedSaltVersion.isEmpty()) {
            throw new BadRequestException(String.format("Salt package version is outdated on the gateway node(s). Please repair the gateway node(s) %s first!",
                    gatewayInstancesWithOutdatedSaltVersion));
        }
    }

    public Set<String> getGatewayInstancesWithOutdatedSaltVersion(StackDto stack) {
        String imageSaltVersion = getImageSaltVersion(stack);
        Set<String> gatewaysUsingOutdatedSaltVersion = new HashSet<>();
        for (InstanceMetadataView instanceMetadata : stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()) {
            String gatewaySaltVersion = getInstanceSaltVersion(instanceMetadata);
            if (StringUtils.isNoneBlank(imageSaltVersion, gatewaySaltVersion) && !imageSaltVersion.equals(gatewaySaltVersion)) {
                LOGGER.warn("Salt package version is outdated on the gateway: {} (image: {}, instance: {})",
                        instanceMetadata.getInstanceId(), imageSaltVersion, gatewaySaltVersion);
                gatewaysUsingOutdatedSaltVersion.add(instanceMetadata.getInstanceId());
            }
        }
        return gatewaysUsingOutdatedSaltVersion;
    }

    public List<SecretRotationFlowChainTriggerEvent> getSaltSecretRotationTriggerEvent(Long stackId) {
        List<SecretType> secretTypes = new ArrayList<>();
        StackDto stack = stackDtoService.getByIdWithoutResources(stackId);
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        if (StringUtils.isNotEmpty(saltSecurityConfig.getLegacySaltSignPublicKey())) {
            secretTypes.add(SALT_SIGN_KEY_PAIR);
        }
        if (stack.getAllAvailableGatewayInstances().size() > 1 && StringUtils.isEmpty(saltSecurityConfig.getSaltMasterPrivateKey())) {
            secretTypes.add(SALT_MASTER_KEY_PAIR);
        }
        if (secretTypes.isEmpty()) {
            LOGGER.info("Secret rotation is not required.");
            return List.of();
        } else {
            LOGGER.info("Secret rotation flow chain trigger added with secret types: {}", secretTypes);
            return List.of(new SecretRotationFlowChainTriggerEvent(EventSelectorUtil.selector(SecretRotationFlowChainTriggerEvent.class),
                    stackId, stack.getResourceCrn(), secretTypes, null, null));
        }
    }

    private boolean isPublicKeyInPemFormat(String publicKeyInBase64) {
        return new String(decodeBase64(publicKeyInBase64)).contains("-----BEGIN PUBLIC KEY-----");
    }

    private String getImageSaltVersion(StackDto stack) {
        try {
            return imageService.getImage(stack.getId()).getPackageVersion(ImagePackageVersion.SALT);
        } catch (CloudbreakImageNotFoundException e) {
            throw new BadRequestException("Image not found", e);
        }
    }

    private String getInstanceSaltVersion(InstanceMetadataView instanceMetadata) {
        try {
            return instanceMetadata.getImage().get(Image.class).getPackageVersion(ImagePackageVersion.SALT);
        } catch (IOException e) {
            LOGGER.warn("Missing image information for instance: " + instanceMetadata.getInstanceId(), e);
            return null;
        }
    }

}

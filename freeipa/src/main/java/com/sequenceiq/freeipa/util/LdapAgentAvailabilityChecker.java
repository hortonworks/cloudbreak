package com.sequenceiq.freeipa.util;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.FreeIpaImageFilterSettings;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;

@Component
public class LdapAgentAvailabilityChecker extends AvailabilityChecker {

    private static final String LDAP_PACKAGE_NAME = "freeipa-ldap-agent";

    private static final Versioned LDAP_PACKAGE_MIN_VERSION_WITH_TLS = () -> "1.1.0.3-b525";

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapAgentAvailabilityChecker.class);

    public boolean isLdapAgentTlsSupportAvailable(Stack stack) {
        return isPackageAvailable(stack, LDAP_PACKAGE_NAME, LDAP_PACKAGE_MIN_VERSION_WITH_TLS)
                && doesAllImageSupportTls(stack);
    }

    private boolean doesAllImageSupportTls(Stack stack) {
        List<Image> imagesList = stack.getNotTerminatedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getImage)
                .map(imageJson -> imageJson.getSilent(Image.class))
                .toList();
        List<String> imageIdsInUse = imagesList.stream().map(Image::getImageId).collect(Collectors.toList());
        String currentImageId = getImageService().getImageForStack(stack).getUuid();
        imageIdsInUse.add(currentImageId);
        return imageIdsInUse.stream().allMatch(s -> s.equals(imageIdsInUse.getFirst()))
                || doesAllNonCurrentImageSupportTls(stack, currentImageId, imageIdsInUse);
    }

    private boolean doesAllNonCurrentImageSupportTls(Stack stack, String currentImageId, List<String> imageIdsInUse) {
        imageIdsInUse.removeAll(List.of(currentImageId));
        FreeIpaImageFilterSettings imageFilterSettings = getImageService().createImageFilterSettingsFromImageEntity(stack);
        try {
            return imageIdsInUse.stream()
                    .distinct()
                    .map(imageFilterSettings::withImageId)
                    .map(imageFilter -> getImageService().getImage(imageFilter))
                    .map(ImageWrapper::getImage)
                    .allMatch(image -> isPackageAvailable(LDAP_PACKAGE_NAME, LDAP_PACKAGE_MIN_VERSION_WITH_TLS, image));
        } catch (ImageNotFoundException e) {
            LOGGER.warn("Image not found, LDAP agent TLS support cannot be determined, returning false");
            return false;
        }
    }

}

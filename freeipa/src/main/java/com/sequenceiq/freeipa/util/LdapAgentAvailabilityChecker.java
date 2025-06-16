package com.sequenceiq.freeipa.util;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class LdapAgentAvailabilityChecker extends AvailabilityChecker {

    private static final String LDAP_PACKAGE_NAME = "freeipa-ldap-agent";

    // only check if package is available, its version does not matter
    private static final Versioned LDAP_PACKAGE_MIN_VERSION_WITH_TLS = () -> "1.1.0.3-b525";

    public boolean isLdapAgentTlsSupportAvailable(Stack stack) {
        return isPackageAvailable(stack, LDAP_PACKAGE_NAME, LDAP_PACKAGE_MIN_VERSION_WITH_TLS)
                && isAllImagesIdenticalWithCurrentImage(stack);
    }

    private boolean isAllImagesIdenticalWithCurrentImage(Stack stack) {
        List<Image> imagesList = stack.getNotDeletedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getImage)
                .map(imageJson -> imageJson.getSilent(Image.class))
                .toList();
        List<String> imageIdsList = imagesList.stream().map(Image::getImageId).collect(Collectors.toList());
        String currentImageId = getImageService().getImageForStack(stack).getUuid();
        imageIdsList.add(currentImageId);
        return imageIdsList.stream().allMatch(s -> s.equals(imageIdsList.getFirst()));
    }

}

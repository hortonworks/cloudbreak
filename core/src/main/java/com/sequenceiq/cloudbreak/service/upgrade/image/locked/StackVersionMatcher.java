package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPOSITORY_VERSION;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;

@Component
public class StackVersionMatcher {

    public boolean isMatchingStackVersion(Image image, Map<String, String> activatedParcels) {
        String stackVersion = activatedParcels.get(StackType.CDH.name());
        return StringUtils.isEmpty(stackVersion) || isStackVersionEquals(image, stackVersion);
    }

    private boolean isStackVersionEquals(Image image, String stackVersion) {
        return stackVersion.equals(mapStackVersionWithDefault(image, stackVersion));
    }

    private String mapStackVersionWithDefault(Image image, String stackVersion) {
        return Optional.ofNullable(image.getStackDetails())
                .map(ImageStackDetails::getRepo)
                .map(StackRepoDetails::getStack)
                .map(stack -> stack.get(REPOSITORY_VERSION))
                .orElse(stackVersion);
    }
}

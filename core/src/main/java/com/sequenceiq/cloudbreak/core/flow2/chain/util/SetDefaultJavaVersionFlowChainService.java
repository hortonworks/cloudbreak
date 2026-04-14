package com.sequenceiq.cloudbreak.core.flow2.chain.util;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.service.ComponentConfigProviderService.RELEASE_VERSION;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionTriggerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.java.vm.AllowableJavaUpdateConfigurations;
import com.sequenceiq.cloudbreak.util.JavaUtil;
import com.sequenceiq.cloudbreak.util.NullUtil;

@Service
public class SetDefaultJavaVersionFlowChainService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetDefaultJavaVersionFlowChainService.class);

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private AllowableJavaUpdateConfigurations allowableJavaUpdateConfigurations;

    public List<SetDefaultJavaVersionTriggerEvent> setDefaultJavaVersionTriggerEvent(StackDto stack, ImageChangeDto imageChangeDto) {
        try {
            StatedImage image = imageCatalogService.getImage(stack.getWorkspaceId(), imageChangeDto.getImageCatalogUrl(),
                    imageChangeDto.getImageCatalogName(), imageChangeDto.getImageId());
            String runtimeVersion = getRuntimeVersion(image.getImage());
            Integer minJavaVersionForRuntime = allowableJavaUpdateConfigurations.getMinJavaVersionForRuntime(runtimeVersion);
            Integer currentJavaVersion = stack.getStack().getJavaVersion();
            String selector = FlowChainTriggers.SET_DEFAULT_JAVA_VERSION_CHAIN_TRIGGER_EVENT;
            if (NullUtil.allNotNull(currentJavaVersion, minJavaVersionForRuntime)) {
                if (currentJavaVersion < minJavaVersionForRuntime) {
                    return List.of(new SetDefaultJavaVersionTriggerEvent(selector, stack.getId(), String.valueOf(minJavaVersionForRuntime),
                            false, false, false));
                } else if (CLOUDERA_STACK_VERSION_7_3_1.getVersion().equalsIgnoreCase(image.getImage().getPackageVersion(ImagePackageVersion.STACK))
                        && currentJavaVersion.equals(JavaUtil.JAVA_11)) {
                    if (CMRepositoryVersionUtil.isVersionOlderThanLimited(runtimeVersion, () -> "7.3.1.500")) {
                        return List.of(new SetDefaultJavaVersionTriggerEvent(selector, stack.getId(),
                                String.valueOf(JavaUtil.JAVA_8), false, false, false));
                    } else {
                        return List.of(new SetDefaultJavaVersionTriggerEvent(
                                selector,
                                stack.getId(),
                                String.valueOf(JavaUtil.JAVA_17),
                                false,
                                false,
                                false
                        ));
                    }
                }
            }
            return List.of();
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.warn("Image not found in image catalog, continue with the upgrade flow.", e);
            throw new NotFoundException("Image not found in image catalog", e);
        } catch (CloudbreakImageCatalogException e) {
            LOGGER.warn("Image catalog is not reachable, continue with the upgrade flow.", e);
            throw new NotFoundException("Image catalog is not reachable", e);
        }
    }

    private String getRuntimeVersion(Image image) {
        return Optional.ofNullable(image.getTags())
                .map(tags -> tags.get(RELEASE_VERSION))
                .or(() -> Optional.ofNullable(image.getPackageVersion(ImagePackageVersion.STACK)))
                .orElse("");
    }
}
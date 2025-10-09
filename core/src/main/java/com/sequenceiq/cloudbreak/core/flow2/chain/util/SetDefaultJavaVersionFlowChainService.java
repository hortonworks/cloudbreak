package com.sequenceiq.cloudbreak.core.flow2.chain.util;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
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

@Service
public class SetDefaultJavaVersionFlowChainService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetDefaultJavaVersionFlowChainService.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private AllowableJavaUpdateConfigurations allowableJavaUpdateConfigurations;

    public List<SetDefaultJavaVersionTriggerEvent> setDefaultJavaVersionTriggerEvent(StackDto stack, ImageChangeDto imageChangeDto) {
        if (entitlementService.isAutoJavaUpgaradeEnabled(stack.getAccountId())) {
            try {
                StatedImage image = imageCatalogService.getImage(stack.getWorkspaceId(), imageChangeDto.getImageCatalogUrl(),
                        imageChangeDto.getImageCatalogName(), imageChangeDto.getImageId());
                String runtimeVersion = image.getImage().getPackageVersion(ImagePackageVersion.STACK);
                Integer minJavaVersionForRuntime = allowableJavaUpdateConfigurations.getMinJavaVersionForRuntime(runtimeVersion);
                Integer currentJavaVersion = stack.getStack().getJavaVersion();
                if (currentJavaVersion != null && minJavaVersionForRuntime != null && currentJavaVersion < minJavaVersionForRuntime) {
                    String selector = FlowChainTriggers.SET_DEFAULT_JAVA_VERSION_CHAIN_TRIGGER_EVENT;
                    return List.of(new SetDefaultJavaVersionTriggerEvent(selector, stack.getId(), String.valueOf(minJavaVersionForRuntime),
                            false, false, false));
                } else {
                    return List.of();
                }
            } catch (CloudbreakImageNotFoundException e) {
                LOGGER.warn("Image not found in image catalog, continue with the upgrade flow.", e);
                throw new NotFoundException("Image not found in image catalog", e);
            } catch (CloudbreakImageCatalogException e) {
                LOGGER.warn("Image catalog is not reachable, continue with the upgrade flow.", e);
                throw new NotFoundException("Image catalog is not reachable", e);
            }
        } else {
            return List.of();
        }
    }
}

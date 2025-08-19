package com.sequenceiq.freeipa.flow.freeipa.enableselinux.handler;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaValidateModifySeLinuxHandlerEvent;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.validation.SeLinuxValidationService;

@Component
public class FreeIpaValidateModifySeLinuxHandler extends ExceptionCatcherEventHandler<FreeIpaValidateModifySeLinuxHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaValidateModifySeLinuxHandler.class);

    private static final String GETENFORCE_COMMAND = "getenforce";

    @Inject
    private StackService stackService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private SeLinuxValidationService seLinuxValidationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaValidateModifySeLinuxHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaValidateModifySeLinuxHandlerEvent> event) {
        LOGGER.warn("Exception while trying to validate stack for updating SELinux to {}, exception: ", event.getData().getSeLinuxMode(), e);
        return new FreeIpaModifySeLinuxFailedEvent(resourceId, "VALIDATION_FAILED", e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<FreeIpaValidateModifySeLinuxHandlerEvent> modifySeLinuxEventEvent) {
        FreeIpaValidateModifySeLinuxHandlerEvent eventData = modifySeLinuxEventEvent.getData();
        LOGGER.debug("Validating if the SeLinux on instances are set to PERMISSIVE mode.");
        Stack stack = stackService.getByIdWithListsInTransaction(eventData.getResourceId());
        validateImageOnFreeIpa(stack);
        validateSeLinuxMode(stack);
        return new FreeIpaModifySeLinuxEvent(FreeIpaModifySeLinuxStateSelectors.MODIFY_SELINUX_FREEIPA_EVENT.selector(),
                eventData.getResourceId(), eventData.getOperationId(), eventData.getSeLinuxMode());
    }

    private void validateSeLinuxMode(Stack stack) {
        LOGGER.debug("Validation request received for SeLinux mode change for stack {}", stack.getResourceCrn());
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfigForSalt(stack);
        try {
            Map<String, String> seLinuxModesMap = hostOrchestrator.runCommandOnAllHosts(primaryGatewayConfig, GETENFORCE_COMMAND);
            List<String> selinuxModes = seLinuxModesMap.values().stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();
            if (selinuxModes.contains(SeLinux.DISABLED.name().toLowerCase(Locale.ROOT))) {
                throw new CloudbreakRuntimeException("SeLinux mode for some instances are in 'disabled' mode.");
            }
        } catch (CloudbreakOrchestratorFailedException exception) {
            LOGGER.warn("Exception while trying to validate SeLinux mode - exception: ", exception);
            throw new CloudbreakRuntimeException("Unable to validate SeLinux modes - connection to instances failed.");
        }
    }

    private void validateImageOnFreeIpa(Stack stack) {
        List<Image> imagesList = stack.getNotDeletedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getImage)
                .map(imageJson -> imageJson.getSilent(Image.class))
                .toList();
        boolean areAllImagesIdentical = imagesList.stream()
                .map(Image::getImageId)
                .distinct()
                .count() == 1;
        if (!areAllImagesIdentical) {
            LOGGER.warn("The images on the FreeIpa instances are different, please consider upgrading the instances to the same image for Stack - {}.",
                    stack.getResourceCrn());
            throw new CloudbreakRuntimeException("The images on the FreeIpa instances are different, " +
                    "please consider upgrading the instances to the same image.");
        }
        Image image = imagesList.getFirst();
        if (OsType.CENTOS7.getOs().equalsIgnoreCase(image.getOs())) {
            LOGGER.warn("The centos7 OS installed on instances is not supported for SELinux 'ENFORCING' mode for Stack - {}.", stack.getResourceCrn());
            throw new CloudbreakRuntimeException("The centos7 OS installed on instances is not supported for SELinux 'ENFORCING' mode.");
        }
        seLinuxValidationService.validateSeLinuxEntitlementGranted(stack);
        seLinuxValidationService.validateSeLinuxSupportedOnTargetImage(stack, image);
    }
}

package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.handler;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreValidateModifySeLinuxHandlerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class CoreValidateModifySeLinuxHandler extends ExceptionCatcherEventHandler<CoreValidateModifySeLinuxHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidateModifySeLinuxHandler.class);

    private static final String GETENFORCE_COMMAND = "getenforce";

    @Inject
    private StackService stackService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CoreValidateModifySeLinuxHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CoreValidateModifySeLinuxHandlerEvent> event) {
        return new CoreModifySeLinuxFailedEvent(resourceId, "VALIDATION_FAILED", e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<CoreValidateModifySeLinuxHandlerEvent> enableSeLinuxEventEvent) {
        CoreValidateModifySeLinuxHandlerEvent eventData = enableSeLinuxEventEvent.getData();
        LOGGER.debug("Validating if the SeLinux on instances are set to PERMISSIVE mode.");
        Stack stack = stackService.getByIdWithListsInTransaction(eventData.getResourceId());
        validateImageOnStack(stack);
        validateSeLinuxMode(stack);
        return new CoreModifySeLinuxEvent(CoreModifySeLinuxStateSelectors.MODIFY_SELINUX_CORE_EVENT.selector(), eventData.getResourceId(),
                eventData.getSelinuxMode());
    }

    private void validateImageOnStack(Stack stack) {
        List<Image> imagesList = stack.getNotDeletedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getImage).map(json -> json.getSilent(Image.class))
                .toList();
        List<String> imageIdsList = imagesList.stream().map(Image::getImageName).toList();
        boolean centos7Image = imagesList.stream().map(Image::getOs).toList().stream()
                .anyMatch(s -> s.equalsIgnoreCase(OsType.CENTOS7.getOs()));
        boolean areAllImagesIdentical = imageIdsList.stream().allMatch(s -> s.equals(imageIdsList.getFirst()));
        if (!areAllImagesIdentical) {
            throw new CloudbreakRuntimeException("The images on the instances are different, please consider upgrading the instances to the same image.");
        }
        if (centos7Image) {
            LOGGER.warn("The centos7 OS installed on instances is not supported for SELinux 'ENFORCING' mode for Stack - {}.", stack.getResourceCrn());
            throw new CloudbreakRuntimeException("The centos7 OS installed on instances is not supported for SELinux 'ENFORCING' mode.");
        }
    }

    private void  validateSeLinuxMode(Stack stack) {
        LOGGER.debug("Validation request received for SeLinux mode change for stack {}", stack.getResourceCrn());
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            Map<String, String> seLinuxModesMap = hostOrchestrator.runCommandOnAllHosts(primaryGatewayConfig, GETENFORCE_COMMAND);
            List<String> selinuxModes = seLinuxModesMap.values().stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();
            if (selinuxModes.contains(SeLinux.DISABLED.name().toLowerCase(Locale.ROOT))) {
                throw new CloudbreakRuntimeException("SeLinux mode for some instances are in 'disabled' mode.");
            }
        } catch (CloudbreakOrchestratorFailedException exception) {
            LOGGER.warn("Exception while trying to validate SeLinux mode - exception: ", exception);
            throw new CloudbreakRuntimeException("Unable to validate SELinux modes - connection to instances failed.");
        }
    }
}

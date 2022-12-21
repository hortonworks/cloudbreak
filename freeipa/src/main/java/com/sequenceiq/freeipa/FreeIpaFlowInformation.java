package com.sequenceiq.freeipa;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationFlowConfig;

@Component
public class FreeIpaFlowInformation implements ApplicationFlowInformation {

    private static final List<String> PARALLEL_FLOWS = List.of(
            FreeIpaCleanupEvent.CLEANUP_EVENT.event(),
            StackTerminationEvent.TERMINATION_EVENT.event(),
            CreateBindUserFlowEvent.CREATE_BIND_USER_EVENT.event(),
            SaltUpdateEvent.SALT_UPDATE_EVENT.event(),
            ImageChangeEvents.IMAGE_CHANGE_EVENT.event(),
            UpscaleFlowEvent.UPSCALE_EVENT.event(),
            DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
            ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event());

    private static final List<Class<? extends FlowConfiguration<?>>> TERMINATION_FLOWS = List.of(StackTerminationFlowConfig.class);

    @Override
    public List<String> getAllowedParallelFlows() {
        return PARALLEL_FLOWS;
    }

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
        return TERMINATION_FLOWS;
    }

}

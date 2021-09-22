package com.sequenceiq.freeipa;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.CreateBindUserFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.config.DiagnosticsCollectionFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowConfig;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootFlowConfig;
import com.sequenceiq.freeipa.flow.stack.image.change.ImageChangeFlowConfig;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionFlowConfig;
import com.sequenceiq.freeipa.flow.stack.start.StackStartFlowConfig;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopFlowConfig;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataFlowConfig;

@Component
public class FreeIpaFlowInformation implements ApplicationFlowInformation {

    private static final List<Class<? extends FlowConfiguration<?>>> RESTARTABLE_FLOWS = List.of(
            ImageChangeFlowConfig.class,
            UpdateUserDataFlowConfig.class,
            StackProvisionFlowConfig.class,
            StackTerminationFlowConfig.class,
            FreeIpaProvisionFlowConfig.class,
            StackStartFlowConfig.class,
            StackStopFlowConfig.class,
            FreeIpaCleanupFlowConfig.class,
            CreateBindUserFlowConfig.class,
            DownscaleFlowConfig.class,
            UpscaleFlowConfig.class,
            ChangePrimaryGatewayFlowConfig.class,
            DiagnosticsCollectionFlowConfig.class,
            RebootFlowConfig.class,
            SaltUpdateFlowConfig.class);

    private static final List<String> PARALLEL_FLOWS = List.of(
            FreeIpaCleanupEvent.CLEANUP_EVENT.event(),
            StackTerminationEvent.TERMINATION_EVENT.event(),
            CreateBindUserFlowEvent.CREATE_BIND_USER_EVENT.event());

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getRestartableFlows() {
        return RESTARTABLE_FLOWS;
    }

    @Override
    public List<String> getAllowedParallelFlows() {
        return PARALLEL_FLOWS;
    }

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
        return List.of(StackTerminationFlowConfig.class);
    }
}

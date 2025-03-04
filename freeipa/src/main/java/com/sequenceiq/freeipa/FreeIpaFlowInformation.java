package com.sequenceiq.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UNKNOWN;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxStateSelectors.SET_SELINUX_TO_ENFORCING_EVENT;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeEvent;
import com.sequenceiq.flow.core.chain.init.config.FlowChainInitEvent;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent;
import com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents;
import com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaFlowInformation implements ApplicationFlowInformation {

    private static final List<String> PARALLEL_FLOWS = List.of(
            RefreshEntitlementParamsEvent.REFRESH_ENTITLEMENT_PARAMS_TRIGGER_EVENT.event(),
            FreeIpaCleanupEvent.CLEANUP_EVENT.event(),
            StackTerminationEvent.TERMINATION_EVENT.event(),
            CreateBindUserFlowEvent.CREATE_BIND_USER_EVENT.event(),
            SaltUpdateEvent.SALT_UPDATE_EVENT.event(),
            ImageChangeEvents.IMAGE_CHANGE_EVENT.event(),
            UpscaleFlowEvent.UPSCALE_EVENT.event(),
            DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
            ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(),
            RebootEvent.REBOOT_EVENT.event(),
            FullBackupEvent.FULL_BACKUP_EVENT.event(),
            DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_SALT_VALIDATION_EVENT.event(),
            FreeIpaVerticalScaleEvent.STACK_VERTICALSCALE_EVENT.event(),
            AwsVariantMigrationEvent.CREATE_RESOURCES_EVENT.event(),
            UpdateUserDataEvents.UPDATE_USERDATA_TRIGGER_EVENT.event(),
            UpgradeCcmStateSelector.UPGRADE_CCM_TRIGGER_EVENT.event(),
            ModifyProxyConfigEvent.MODIFY_PROXY_TRIGGER_EVENT.event(),
            FlowChainInitEvent.FLOWCHAIN_INIT_TRIGGER_EVENT.event(),
            FlowChainFinalizeEvent.FLOWCHAIN_FINALIZE_TRIGGER_EVENT.event(),
            FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT.event(),
            SET_SELINUX_TO_ENFORCING_EVENT.event());

    private static final List<Class<? extends FlowConfiguration<?>>> TERMINATION_FLOWS = List.of(StackTerminationFlowConfig.class);

    @Inject
    private StackService stackService;

    @Override
    public List<String> getAllowedParallelFlows() {
        return PARALLEL_FLOWS;
    }

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
        return TERMINATION_FLOWS;
    }

    @Override
    public void handleFlowFail(FlowLog flowLog) {
        Stack stack = stackService.getStackById(flowLog.getResourceId());
        LOGGER.info("Handling failed freeipa flow {} for {}", flowLog, stack.getName());
        if (stack.getStackStatus() != null && stack.getStackStatus().getStatus() != null) {
            stack.setStackStatus(new StackStatus(stack, stack.getStackStatus().getStatus().mapToFailedIfInProgress(), "Flow failed", UNKNOWN));
            stackService.save(stack);
        }
    }

}

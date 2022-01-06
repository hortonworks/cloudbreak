package com.sequenceiq.cloudbreak.core.flow2;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigUpdateFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.migration.AwsVariantMigrationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleConfig;
import com.sequenceiq.cloudbreak.domain.stack.StackBase;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowLog;

@Component
public class CloudbreakFlowInformation implements ApplicationFlowInformation {

    private static final List<String> ALLOWED_PARALLEL_FLOWS = List.of(
            ClusterTerminationEvent.TERMINATION_EVENT.event(),
            ExternalDatabaseTerminationEvent.START_EXTERNAL_DATABASE_TERMINATION_EVENT.event(),
            StackTerminationEvent.TERMINATION_EVENT.event(),
            ClusterTerminationEvent.PROPER_TERMINATION_EVENT.event());

    private static final List<Class<? extends FlowConfiguration<?>>> RESTARTABLE_FLOWS = Arrays.asList(
            StackCreationFlowConfig.class,
            StackSyncFlowConfig.class, StackTerminationFlowConfig.class, StackStopFlowConfig.class, StackStartFlowConfig.class,
            ExternalDatabaseStartFlowConfig.class, ExternalDatabaseStopFlowConfig.class,
            StackUpscaleConfig.class, StackDownscaleConfig.class,
            InstanceTerminationFlowConfig.class,
            ManualStackRepairTriggerFlowConfig.class,
            ClusterCreationFlowConfig.class,
            ClusterSyncFlowConfig.class, ClusterTerminationFlowConfig.class, ClusterCredentialChangeFlowConfig.class,
            ClusterStartFlowConfig.class, ClusterStopFlowConfig.class,
            ClusterUpscaleFlowConfig.class, ClusterDownscaleFlowConfig.class,
            ClusterUpgradeFlowConfig.class, ClusterResetFlowConfig.class, ChangePrimaryGatewayFlowConfig.class,
            ClusterCertificateRenewFlowConfig.class, DatabaseBackupFlowConfig.class, DatabaseRestoreFlowConfig.class, PillarConfigUpdateFlowConfig.class,
            AwsVariantMigrationFlowConfig.class
    );

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getRestartableFlows() {
        return RESTARTABLE_FLOWS;
    }

    @Override
    public List<String> getAllowedParallelFlows() {
        return ALLOWED_PARALLEL_FLOWS;
    }

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
        return Arrays.asList(StackTerminationFlowConfig.class, ClusterTerminationFlowConfig.class, ExternalDatabaseTerminationFlowConfig.class);
    }

    @Override
    public void handleFlowFail(FlowLog flowLog) {
        StackBase stack = stackService.getStackBaseById(flowLog.getResourceId());
        if (stack.getStatus() != null && stack.getStackStatus().getDetailedStackStatus() != null) {
            stackUpdater.updateStackStatusAndSetDetailedStatusToUnknown(flowLog.getResourceId(), stack.getStatus().mapToFailedIfInProgress(), "Flow failed");
        }
    }
}

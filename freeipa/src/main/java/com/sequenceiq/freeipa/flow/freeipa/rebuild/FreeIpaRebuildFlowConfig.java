package com.sequenceiq.freeipa.flow.freeipa.rebuild;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.FREEIPA_REBUILD_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.FREEIPA_REBUILD_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.FREEIPA_REBUILD_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.ADD_INSTANCE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.ADD_INSTANCE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.ADD_INSTANCE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.BOOTSTRAP_MACHINES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.BOOTSTRAP_MACHINES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.CLUSTER_PROXY_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.CLUSTER_PROXY_REGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.COLLECT_RESOURCES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.COLLECT_RESOURCES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.EXTEND_METADATA_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.EXTEND_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.FREEIPA_CLEANUP_AFTER_RESTORE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.FREEIPA_CLEANUP_AFTER_RESTORE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.FREEIPA_INSTALL_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.FREEIPA_INSTALL_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.FREEIPA_POST_INSTALL_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.FREEIPA_POST_INSTALL_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.FREEIPA_RESTORE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.FREEIPA_RESTORE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.ORCHESTRATOR_CONFIG_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.ORCHESTRATOR_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REBOOT_TRIGGERED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REBOOT_TRIGGER_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REBOOT_WAIT_UNTIL_AVAILABLE_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REBOOT_WAIT_UNTIL_AVAILABLE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REBUILD_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REBUILD_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REBUILD_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REBUILD_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REBUILD_STARTED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REMOVE_INSTANCES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REMOVE_INSTANCES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.SAVE_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.TLS_SETUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.UPDATE_LOAD_BALANCER_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.UPDATE_LOAD_BALANCER_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.UPDATE_METADATA_FOR_DELETION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.VALIDATE_HEALTH_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.VALIDATE_HEALTH_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.VALIDATE_INSTANCE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.VALIDATING_BACKUP_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.VALIDATING_BACKUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.VALIDATING_CLOUD_STORAGE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.VALIDATING_CLOUD_STORAGE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_ADD_INSTANCE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_BOOTSTRAPPING_MACHINES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_CLEANUP_FREEIPA_AFTER_RESTORE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_COLLECT_RESOURCES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_EXTEND_METADATA_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_FREEIPA_INSTALL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_FREEIPA_POST_INSTALL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_ORCHESTRATOR_CONFIG_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_REBOOT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_REMOVE_INSTANCES_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_REMOVE_INSTANCES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_RESTORE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_SAVE_METADATA_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_START_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_TLS_SETUP_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_UPDATE_CLUSTERPROXY_REGISTRATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_UPDATE_ENVIRONMENT_STACK_CONFIG_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_UPDATE_KERBEROS_NAMESERVERS_CONFIG_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_UPDATE_LOAD_BALANCER_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_UPDATE_METADATA_FOR_DELETION_REQUEST_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_VALIDATE_BACKUP_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_VALIDATE_CLOUD_STORAGE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_VALIDATE_HEALTH_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_VALIDATE_INSTANCE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState.REBUILD_WAIT_UNTIL_AVAILABLE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class FreeIpaRebuildFlowConfig extends AbstractFlowConfiguration<FreeIpaRebuildState, FreeIpaRebuildFlowEvent>
        implements RetryableFlowConfiguration<FreeIpaRebuildFlowEvent>, FreeIpaUseCaseAware {

    private static final List<Transition<FreeIpaRebuildState, FreeIpaRebuildFlowEvent>> TRANSITIONS =
            new Transition.Builder<FreeIpaRebuildState, FreeIpaRebuildFlowEvent>()
                    .defaultFailureEvent(REBUILD_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(REBUILD_START_STATE)
                    .event(REBUILD_EVENT)
                    .defaultFailureEvent()

                    .from(REBUILD_START_STATE)
                    .to(REBUILD_UPDATE_METADATA_FOR_DELETION_REQUEST_STATE)
                    .event(REBUILD_STARTED_EVENT)
                    .defaultFailureEvent()

                    .from(REBUILD_UPDATE_METADATA_FOR_DELETION_REQUEST_STATE)
                    .to(REBUILD_COLLECT_RESOURCES_STATE)
                    .event(UPDATE_METADATA_FOR_DELETION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(REBUILD_COLLECT_RESOURCES_STATE)
                    .to(REBUILD_REMOVE_INSTANCES_STATE)
                    .event(COLLECT_RESOURCES_FINISHED_EVENT)
                    .failureEvent(COLLECT_RESOURCES_FAILED_EVENT)

                    .from(REBUILD_REMOVE_INSTANCES_STATE)
                    .to(REBUILD_REMOVE_INSTANCES_FINISHED_STATE)
                    .event(REMOVE_INSTANCES_FINISHED_EVENT)
                    .failureEvent(REMOVE_INSTANCES_FAILED_EVENT)

                    .from(REBUILD_REMOVE_INSTANCES_FINISHED_STATE)
                    .to(REBUILD_ADD_INSTANCE_STATE)
                    .event(ADD_INSTANCE_EVENT)
                    .defaultFailureEvent()

                    .from(REBUILD_ADD_INSTANCE_STATE)
                    .to(REBUILD_VALIDATE_INSTANCE_STATE)
                    .event(ADD_INSTANCE_FINISHED_EVENT)
                    .failureEvent(ADD_INSTANCE_FAILED_EVENT)

                    .from(REBUILD_VALIDATE_INSTANCE_STATE)
                    .to(REBUILD_EXTEND_METADATA_STATE)
                    .event(VALIDATE_INSTANCE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(REBUILD_EXTEND_METADATA_STATE)
                    .to(REBUILD_SAVE_METADATA_STATE)
                    .event(EXTEND_METADATA_FINISHED_EVENT)
                    .failureEvent(EXTEND_METADATA_FAILED_EVENT)

                    .from(REBUILD_SAVE_METADATA_STATE)
                    .to(REBUILD_TLS_SETUP_STATE)
                    .event(SAVE_METADATA_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(REBUILD_TLS_SETUP_STATE)
                    .to(REBUILD_UPDATE_CLUSTERPROXY_REGISTRATION_STATE)
                    .event(TLS_SETUP_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(REBUILD_UPDATE_CLUSTERPROXY_REGISTRATION_STATE)
                    .to(REBUILD_BOOTSTRAPPING_MACHINES_STATE)
                    .event(CLUSTER_PROXY_REGISTRATION_FINISHED_EVENT)
                    .failureEvent(CLUSTER_PROXY_REGISTRATION_FAILED_EVENT)

                    .from(REBUILD_BOOTSTRAPPING_MACHINES_STATE)
                    .to(REBUILD_ORCHESTRATOR_CONFIG_STATE)
                    .event(BOOTSTRAP_MACHINES_FINISHED_EVENT)
                    .failureEvent(BOOTSTRAP_MACHINES_FAILED_EVENT)

                    .from(REBUILD_ORCHESTRATOR_CONFIG_STATE)
                    .to(REBUILD_VALIDATE_CLOUD_STORAGE_STATE)
                    .event(ORCHESTRATOR_CONFIG_FINISHED_EVENT)
                    .failureEvent(ORCHESTRATOR_CONFIG_FAILED_EVENT)

                    .from(REBUILD_VALIDATE_CLOUD_STORAGE_STATE)
                    .to(REBUILD_VALIDATE_BACKUP_STATE)
                    .event(VALIDATING_CLOUD_STORAGE_FINISHED_EVENT)
                    .failureEvent(VALIDATING_CLOUD_STORAGE_FAILED_EVENT)

                    .from(REBUILD_VALIDATE_BACKUP_STATE)
                    .to(REBUILD_FREEIPA_INSTALL_STATE)
                    .event(VALIDATING_BACKUP_FINISHED_EVENT)
                    .failureEvent(VALIDATING_BACKUP_FAILED_EVENT)

                    .from(REBUILD_FREEIPA_INSTALL_STATE)
                    .to(REBUILD_RESTORE_STATE)
                    .event(FREEIPA_INSTALL_FINISHED_EVENT)
                    .failureEvent(FREEIPA_INSTALL_FAILED_EVENT)

                    .from(REBUILD_RESTORE_STATE)
                    .to(REBUILD_REBOOT_STATE)
                    .event(FREEIPA_RESTORE_FINISHED_EVENT)
                    .failureEvent(FREEIPA_RESTORE_FAILED_EVENT)

                    .from(REBUILD_REBOOT_STATE)
                    .to(REBUILD_WAIT_UNTIL_AVAILABLE_STATE)
                    .event(REBOOT_TRIGGERED_EVENT)
                    .failureEvent(REBOOT_TRIGGER_FAILURE_EVENT)

                    .from(REBUILD_WAIT_UNTIL_AVAILABLE_STATE)
                    .to(REBUILD_CLEANUP_FREEIPA_AFTER_RESTORE_STATE)
                    .event(REBOOT_WAIT_UNTIL_AVAILABLE_FINISHED_EVENT)
                    .failureEvent(REBOOT_WAIT_UNTIL_AVAILABLE_FAILURE_EVENT)

                    .from(REBUILD_CLEANUP_FREEIPA_AFTER_RESTORE_STATE)
                    .to(REBUILD_FREEIPA_POST_INSTALL_STATE)
                    .event(FREEIPA_CLEANUP_AFTER_RESTORE_FINISHED_EVENT)
                    .failureEvent(FREEIPA_CLEANUP_AFTER_RESTORE_FAILED_EVENT)

                    .from(REBUILD_FREEIPA_POST_INSTALL_STATE)
                    .to(REBUILD_VALIDATE_HEALTH_STATE)
                    .event(FREEIPA_POST_INSTALL_FINISHED_EVENT)
                    .failureEvent(FREEIPA_POST_INSTALL_FAILED_EVENT)

                    .from(REBUILD_VALIDATE_HEALTH_STATE)
                    .to(REBUILD_UPDATE_KERBEROS_NAMESERVERS_CONFIG_STATE)
                    .event(VALIDATE_HEALTH_FINISHED_EVENT)
                    .failureEvent(VALIDATE_HEALTH_FAILED_EVENT)

                    .from(REBUILD_UPDATE_KERBEROS_NAMESERVERS_CONFIG_STATE)
                    .to(REBUILD_UPDATE_LOAD_BALANCER_STATE)
                    .event(UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT)
                    .failureEvent(UPDATE_LOAD_BALANCER_FAILED_EVENT)

                    .from(REBUILD_UPDATE_LOAD_BALANCER_STATE)
                    .to(REBUILD_UPDATE_ENVIRONMENT_STACK_CONFIG_STATE)
                    .event(UPDATE_LOAD_BALANCER_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(REBUILD_UPDATE_ENVIRONMENT_STACK_CONFIG_STATE)
                    .to(REBUILD_FINISHED_STATE)
                    .event(UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(REBUILD_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(REBUILD_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FreeIpaRebuildState, FreeIpaRebuildFlowEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            REBUILD_FAILED_STATE, REBUILD_FAILURE_HANDLED_EVENT);

    protected FreeIpaRebuildFlowConfig() {
        super(FreeIpaRebuildState.class, FreeIpaRebuildFlowEvent.class);
    }

    @Override
    protected List<Transition<FreeIpaRebuildState, FreeIpaRebuildFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaRebuildState, FreeIpaRebuildFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaRebuildFlowEvent[] getEvents() {
        return FreeIpaRebuildFlowEvent.values();
    }

    @Override
    public FreeIpaRebuildFlowEvent[] getInitEvents() {
        return new FreeIpaRebuildFlowEvent[]{REBUILD_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Rebuild FreeIPA";
    }

    @Override
    public FreeIpaRebuildFlowEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }

    @Override
    public UsageProto.CDPFreeIPAStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return FREEIPA_REBUILD_STARTED;
        } else if (REBUILD_FINISHED_STATE.equals(flowState)) {
            return FREEIPA_REBUILD_FINISHED;
        } else if (REBUILD_FAILED_STATE.equals(flowState)) {
            return FREEIPA_REBUILD_FAILED;
        } else {
            return UNSET;
        }
    }
}

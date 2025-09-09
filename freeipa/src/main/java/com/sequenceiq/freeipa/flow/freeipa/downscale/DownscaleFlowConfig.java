package com.sequenceiq.freeipa.flow.freeipa.downscale;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.DOWNSCALE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.DOWNSCALE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.DOWNSCALE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.CLUSTERPROXY_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.CLUSTERPROXY_REGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.COLLECT_RESOURCES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.COLLECT_RESOURCES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_ADD_ADDITIONAL_HOSTNAMES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_REMOVE_USERDATA_SECRETS_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_UPDATE_DNS_SOA_RECORDS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_UPDATE_DNS_SOA_RECORDS_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REMOVE_DNS_ENTRIES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REMOVE_DNS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REMOVE_HOSTS_FROM_ORCHESTRATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REMOVE_HOSTS_FROM_ORCHESTRATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REMOVE_INSTANCES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REMOVE_INSTANCES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REMOVE_REPLICATION_AGREEMENTS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REMOVE_REPLICATION_AGREEMENTS_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REMOVE_SERVERS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REMOVE_SERVERS_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REVOKE_CERTS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REVOKE_CERTS_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.STARTING_DOWNSCALE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.STOP_HEALTH_AGENT_FINISHED;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.STOP_TELEMETRY_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.UPDATE_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.UPDATE_METADATA_FOR_DELETION_REQUEST_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_ADD_ADDITIONAL_HOSTNAMES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_CLUSTERPROXY_REGISTRATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_COLLECT_RESOURCES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_FAIL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_REMOVE_DNS_ENTRIES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_REMOVE_HOSTS_FROM_ORCHESTRATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_REMOVE_INSTANCES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_REMOVE_REPLICATION_AGREEMENTS_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_REMOVE_SERVERS_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_REMOVE_USERDATA_SECRETS_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_REVOKE_CERTS_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_STOP_HEALTH_AGENT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_STOP_TELEMETRY_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_UPDATE_DNS_SOA_RECORDS_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_UPDATE_METADATA_FOR_DELETION_REQUEST_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.DOWNSCALE_UPDATE_METADATA_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState.STARTING_DOWNSCALE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class DownscaleFlowConfig extends StackStatusFinalizerAbstractFlowConfig<DownscaleState, DownscaleFlowEvent>
        implements RetryableFlowConfiguration<DownscaleFlowEvent>, FreeIpaUseCaseAware {
    private static final List<Transition<DownscaleState, DownscaleFlowEvent>> TRANSITIONS =
            new Transition.Builder<DownscaleState, DownscaleFlowEvent>()
                    .defaultFailureEvent(FAILURE_EVENT)

                    .from(INIT_STATE).to(STARTING_DOWNSCALE_STATE)
                    .event(DOWNSCALE_EVENT)
                    .defaultFailureEvent()

                    .from(STARTING_DOWNSCALE_STATE).to(DOWNSCALE_UPDATE_METADATA_FOR_DELETION_REQUEST_STATE)
                    .event(STARTING_DOWNSCALE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(DOWNSCALE_UPDATE_METADATA_FOR_DELETION_REQUEST_STATE).to(DOWNSCALE_CLUSTERPROXY_REGISTRATION_STATE)
                    .event(UPDATE_METADATA_FOR_DELETION_REQUEST_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(DOWNSCALE_CLUSTERPROXY_REGISTRATION_STATE).to(DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_STATE)
                    .event(CLUSTERPROXY_REGISTRATION_FINISHED_EVENT)
                    .failureEvent(CLUSTERPROXY_REGISTRATION_FAILED_EVENT)

                    .from(DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_STATE).to(DOWNSCALE_ADD_ADDITIONAL_HOSTNAMES_STATE)
                    .event(DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_FINISHED_EVENT)
                    .failureEvent(DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_FAILED_EVENT)

                    .from(DOWNSCALE_ADD_ADDITIONAL_HOSTNAMES_STATE).to(DOWNSCALE_STOP_HEALTH_AGENT_STATE)
                    .event(DOWNSCALE_ADD_ADDITIONAL_HOSTNAMES_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(DOWNSCALE_STOP_HEALTH_AGENT_STATE).to(DOWNSCALE_STOP_TELEMETRY_STATE)
                    .event(STOP_HEALTH_AGENT_FINISHED)
                    .defaultFailureEvent()

                    .from(DOWNSCALE_STOP_TELEMETRY_STATE).to(DOWNSCALE_REMOVE_USERDATA_SECRETS_STATE)
                    .event(STOP_TELEMETRY_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(DOWNSCALE_REMOVE_USERDATA_SECRETS_STATE).to(DOWNSCALE_COLLECT_RESOURCES_STATE)
                    .event(DOWNSCALE_REMOVE_USERDATA_SECRETS_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(DOWNSCALE_COLLECT_RESOURCES_STATE).to(DOWNSCALE_REMOVE_INSTANCES_STATE)
                    .event(COLLECT_RESOURCES_FINISHED_EVENT)
                    .failureEvent(COLLECT_RESOURCES_FAILED_EVENT)

                    .from(DOWNSCALE_REMOVE_INSTANCES_STATE).to(DOWNSCALE_REMOVE_SERVERS_STATE)
                    .event(REMOVE_INSTANCES_FINISHED_EVENT)
                    .failureEvent(REMOVE_INSTANCES_FAILED_EVENT)

                    .from(DOWNSCALE_REMOVE_SERVERS_STATE).to(DOWNSCALE_REMOVE_REPLICATION_AGREEMENTS_STATE)
                    .event(REMOVE_SERVERS_FINISHED_EVENT)
                    .failureEvent(REMOVE_SERVERS_FAILED_EVENT)

                    .from(DOWNSCALE_REMOVE_REPLICATION_AGREEMENTS_STATE).to(DOWNSCALE_REVOKE_CERTS_STATE)
                    .event(REMOVE_REPLICATION_AGREEMENTS_FINISHED_EVENT)
                    .failureEvent(REMOVE_REPLICATION_AGREEMENTS_FAILED_EVENT)

                    .from(DOWNSCALE_REVOKE_CERTS_STATE).to(DOWNSCALE_REMOVE_DNS_ENTRIES_STATE)
                    .event(REVOKE_CERTS_FINISHED_EVENT)
                    .failureEvent(REVOKE_CERTS_FAILED_EVENT)

                    .from(DOWNSCALE_REMOVE_DNS_ENTRIES_STATE).to(DOWNSCALE_UPDATE_DNS_SOA_RECORDS_STATE)
                    .event(REMOVE_DNS_ENTRIES_FINISHED_EVENT)
                    .failureEvent(REMOVE_DNS_FAILED_EVENT)

                    .from(DOWNSCALE_UPDATE_DNS_SOA_RECORDS_STATE).to(DOWNSCALE_REMOVE_HOSTS_FROM_ORCHESTRATION_STATE)
                    .event(DOWNSCALE_UPDATE_DNS_SOA_RECORDS_FINISHED_EVENT)
                    .failureEvent(DOWNSCALE_UPDATE_DNS_SOA_RECORDS_FAILED_EVENT)

                    .from(DOWNSCALE_REMOVE_HOSTS_FROM_ORCHESTRATION_STATE).to(DOWNSCALE_UPDATE_METADATA_STATE)
                    .event(REMOVE_HOSTS_FROM_ORCHESTRATION_FINISHED_EVENT)
                    .failureEvent(REMOVE_HOSTS_FROM_ORCHESTRATION_FAILED_EVENT)

                    .from(DOWNSCALE_UPDATE_METADATA_STATE).to(DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_STATE)
                    .event(UPDATE_METADATA_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_STATE).to(DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_STATE)
                    .event(DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT)
                    .failureEvent(DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FAILED_EVENT)

                    .from(DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_STATE).to(DOWNSCALE_FINISHED_STATE)
                    .event(DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT)
                    .failureEvent(DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FAILED_EVENT)

                    .from(DOWNSCALE_FINISHED_STATE).to(FINAL_STATE)
                    .event(DOWNSCALE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DownscaleState, DownscaleFlowEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            DOWNSCALE_FAIL_STATE, FAIL_HANDLED_EVENT);

    public DownscaleFlowConfig() {
        super(DownscaleState.class, DownscaleFlowEvent.class);
    }

    @Override
    protected List<Transition<DownscaleState, DownscaleFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DownscaleState, DownscaleFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DownscaleFlowEvent[] getEvents() {
        return DownscaleFlowEvent.values();
    }

    @Override
    public DownscaleFlowEvent[] getInitEvents() {
        return new DownscaleFlowEvent[] { DOWNSCALE_EVENT };
    }

    @Override
    public String getDisplayName() {
        return "Downscale FreeIPA";
    }

    @Override
    public DownscaleFlowEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return DOWNSCALE_STARTED;
        } else if (DOWNSCALE_FINISHED_STATE.equals(flowState)) {
            return DOWNSCALE_FINISHED;
        } else if (DOWNSCALE_FAIL_STATE.equals(flowState)) {
            return DOWNSCALE_FAILED;
        } else {
            return UNSET;
        }
    }
}

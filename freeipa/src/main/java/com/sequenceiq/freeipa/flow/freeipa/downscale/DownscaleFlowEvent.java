package com.sequenceiq.freeipa.flow.freeipa.downscale;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert.RevokeCertsResponse;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns.RemoveDnsResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.collecthostnames.CollectAdditionalHostnamesResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords.UpdateDnsSoaRecordsResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removehosts.RemoveHostsFromOrchestrationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removereplication.RemoveReplicationAgreementsResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removeserver.RemoveServersResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stoptelemetry.StopTelemetryResponse;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationSuccess;

public enum DownscaleFlowEvent implements FlowEvent {
    DOWNSCALE_EVENT("DOWNSCALE_EVENT"),
    STARTING_DOWNSCALE_FINISHED_EVENT("STARTING_DOWNSCALE_FINISHED_EVENT"),
    UPDATE_METADATA_FOR_DELETION_REQUEST_FINISHED_EVENT("UPDATE_METADATA_FOR_DELETION_REQUEST_FINISHED_EVENT"),
    CLUSTERPROXY_REGISTRATION_FINISHED_EVENT(EventSelectorUtil.selector(ClusterProxyUpdateRegistrationSuccess.class)),
    CLUSTERPROXY_REGISTRATION_FAILED_EVENT(EventSelectorUtil.selector(ClusterProxyUpdateRegistrationFailed.class)),
    DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_FINISHED_EVENT(EventSelectorUtil.selector(CollectAdditionalHostnamesResponse.class)),
    DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_FAILED_EVENT("DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_FAILED_EVENT"),
    DOWNSCALE_ADD_ADDITIONAL_HOSTNAMES_FINISHED_EVENT("DOWNSCALE_ADD_ADDITIONAL_HOSTNAMES_FINISHED_EVENT"),
    STOP_TELEMETRY_FINISHED_EVENT(EventSelectorUtil.selector(StopTelemetryResponse.class)),
    COLLECT_RESOURCES_FINISHED_EVENT(EventSelectorUtil.selector(DownscaleStackCollectResourcesResult.class)),
    COLLECT_RESOURCES_FAILED_EVENT(EventSelectorUtil.failureSelector(DownscaleStackCollectResourcesResult.class)),
    REMOVE_INSTANCES_FINISHED_EVENT(EventSelectorUtil.selector(DownscaleStackResult.class)),
    REMOVE_INSTANCES_FAILED_EVENT(EventSelectorUtil.failureSelector(DownscaleStackResult.class)),
    REMOVE_SERVERS_FINISHED_EVENT(EventSelectorUtil.selector(RemoveServersResponse.class)),
    REMOVE_SERVERS_FAILED_EVENT(EventSelectorUtil.failureSelector(RemoveServersResponse.class)),
    REMOVE_REPLICATION_AGREEMENTS_FINISHED_EVENT(EventSelectorUtil.selector(RemoveReplicationAgreementsResponse.class)),
    REMOVE_REPLICATION_AGREEMENTS_FAILED_EVENT("REMOVE_REPLICATION_AGREEMENTS_FAILED_EVENT"),
    REVOKE_CERTS_FINISHED_EVENT(EventSelectorUtil.selector(RevokeCertsResponse.class)),
    REVOKE_CERTS_FAILED_EVENT(EventSelectorUtil.failureSelector(RevokeCertsResponse.class)),
    REMOVE_DNS_ENTRIES_FINISHED_EVENT(EventSelectorUtil.selector(RemoveDnsResponse.class)),
    REMOVE_DNS_FAILED_EVENT(EventSelectorUtil.failureSelector(RemoveDnsResponse.class)),
    DOWNSCALE_UPDATE_DNS_SOA_RECORDS_FINISHED_EVENT(EventSelectorUtil.selector(UpdateDnsSoaRecordsResponse.class)),
    DOWNSCALE_UPDATE_DNS_SOA_RECORDS_FAILED_EVENT("DOWNSCALE_UPDATE_DNS_SOA_RECORDS_FAILED_EVENT"),
    UPDATE_METADATA_FINISHED_EVENT("UPDATE_METADATA_FINISHED_EVENT"),
    REMOVE_HOSTS_FROM_ORCHESTRATION_FINISHED_EVENT(EventSelectorUtil.selector(RemoveHostsFromOrchestrationSuccess.class)),
    REMOVE_HOSTS_FROM_ORCHESTRATION_FAILED_EVENT("REMOVE_HOSTS_FROM_ORCHESTRATION_FAILED_EVENT"),
    DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT("DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT"),
    DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FAILED_EVENT("DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FAILED_EVENT"),
    DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT("DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT"),
    DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FAILED_EVENT("DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FAILED_EVENT"),
    DOWNSCALE_FINISHED_EVENT("DOWNSCALE_FINISHED_EVENT"),
    FAILURE_EVENT("DOWNSCALE_FAILURE_EVENT"),
    FAIL_HANDLED_EVENT("DOWNSCALE_FAIL_HANDLED_EVENT");

    private final String event;

    DownscaleFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}

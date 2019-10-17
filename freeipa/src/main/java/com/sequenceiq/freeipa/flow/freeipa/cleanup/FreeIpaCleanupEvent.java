package com.sequenceiq.freeipa.flow.freeipa.cleanup;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert.RevokeCertsResponse;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns.RemoveDnsResponse;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.host.RemoveHostsResponse;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.roles.RemoveRolesResponse;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.users.RemoveUsersResponse;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.vault.RemoveVaultEntriesResponse;

public enum FreeIpaCleanupEvent implements FlowEvent {
    CLEANUP_EVENT("CLEANUP_EVENT"),
    REVOKE_CERTS_FINISHED_EVENT(EventSelectorUtil.selector(RevokeCertsResponse.class)),
    REVOKE_CERTS_FAILED_EVENT(EventSelectorUtil.failureSelector(RevokeCertsResponse.class)),
    REMOVE_HOSTS_FINISHED_EVENT(EventSelectorUtil.selector(RemoveHostsResponse.class)),
    REMOVE_HOSTS_FAILED_EVENT(EventSelectorUtil.failureSelector(RemoveHostsResponse.class)),
    REMOVE_DNS_ENTRIES_FINISHED_EVENT(EventSelectorUtil.selector(RemoveDnsResponse.class)),
    REMOVE_DNS_FAILED_EVENT(EventSelectorUtil.failureSelector(RemoveDnsResponse.class)),
    REMOVE_VAULT_ENTRIES_FINISHED_EVENT(EventSelectorUtil.selector(RemoveVaultEntriesResponse.class)),
    REMOVE_VAULT_ENTRIES_FAILED_EVENT(EventSelectorUtil.failureSelector(RemoveVaultEntriesResponse.class)),
    REMOVE_USERS_FINISHED_EVENT(EventSelectorUtil.selector(RemoveUsersResponse.class)),
    REMOVE_USERS_FAILED_EVENT(EventSelectorUtil.failureSelector(RemoveUsersResponse.class)),
    REMOVE_ROLES_FINISHED_EVENT(EventSelectorUtil.selector(RemoveRolesResponse.class)),
    REMOVE_ROLES_FAILED_EVENT(EventSelectorUtil.failureSelector(RemoveRolesResponse.class)),
    CLEANUP_FINISHED_EVENT("CLEANUP_FINISHED_EVENT"),
    CLEANUP_FAILED_EVENT("CLEANUP_FAILED_EVENT"),
    CLEANUP_FAILURE_HANDLED_EVENT("CLEANUP_FAILURE_HANDLED_EVENT");

    private final String event;

    FreeIpaCleanupEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}

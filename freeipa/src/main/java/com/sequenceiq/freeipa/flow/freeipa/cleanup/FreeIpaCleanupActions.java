package com.sequenceiq.freeipa.flow.freeipa.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert.RevokeCertsRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert.RevokeCertsResponse;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns.RemoveDnsRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns.RemoveDnsResponse;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure.CleanupFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure.RemoveDnsResponseToCleanupFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure.RemoveHostsResponseToCleanupFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure.RemoveRolesResponseToCleanupFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure.RemoveUsersResponseToCleanupFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure.RemoveVaultEntriesResponseToCleanupFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure.RevokeCertsResponseToCleanupFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.host.RemoveHostsRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.host.RemoveHostsResponse;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.roles.RemoveRolesRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.roles.RemoveRolesResponse;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.users.RemoveUsersRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.users.RemoveUsersResponse;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.vault.RemoveVaultEntriesRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.vault.RemoveVaultEntriesResponse;
import com.sequenceiq.freeipa.service.operation.OperationStatusService;

@Configuration
public class FreeIpaCleanupActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCleanupActions.class);

    @Bean(name = "REVOKE_CERTS_STATE")
    public Action<?, ?> revokeCertsAction() {
        return new AbstractFreeIpaCleanupAction<>(CleanupEvent.class) {
            @Override
            protected void doExecute(FreeIpaContext context, CleanupEvent payload, Map<Object, Object> variables) {
                if (payload.getHosts() == null || payload.getHosts().isEmpty()) {
                    LOGGER.info("Host is empty, skipping revoking certificates");
                    RevokeCertsResponse response =
                            new RevokeCertsResponse(payload, Collections.emptySet(), Collections.emptyMap());
                    sendEvent(context, response);
                } else {
                    RevokeCertsRequest request = new RevokeCertsRequest(payload, context.getStack(), context.getFreeIpa());
                    sendEvent(context, request);
                }
            }
        };
    }

    @Bean(name = "REMOVE_HOSTS_STATE")
    public Action<?, ?> removeHostsAction() {
        return new AbstractFreeIpaCleanupAction<>(RevokeCertsResponse.class) {
            @Override
            protected void doExecute(FreeIpaContext context, RevokeCertsResponse payload, Map<Object, Object> variables) {
                if (payload.getHosts() == null || payload.getHosts().isEmpty()) {
                    LOGGER.info("Host is empty, skipping removing hosts");
                    RemoveHostsResponse response =
                            new RemoveHostsResponse(payload, Collections.emptySet(), Collections.emptyMap());
                    sendEvent(context, response);
                } else {
                    RemoveHostsRequest request = new RemoveHostsRequest(payload, context.getStack(), context.getFreeIpa());
                    sendEvent(context, request);
                }
            }
        };
    }

    @Bean(name = "REMOVE_DNS_ENTRIES_STATE")
    public Action<?, ?> removeDnsEntriesAction() {
        return new AbstractFreeIpaCleanupAction<>(RemoveHostsResponse.class) {
            @Override
            protected void doExecute(FreeIpaContext context, RemoveHostsResponse payload, Map<Object, Object> variables) {
                if (payload.getHosts() == null || payload.getHosts().isEmpty()) {
                    LOGGER.info("Host is empty, skipping removing hosts");
                    RemoveDnsResponse response =
                            new RemoveDnsResponse(payload, Collections.emptySet(), Collections.emptyMap());
                    sendEvent(context, response);
                } else {
                    RemoveDnsRequest request = new RemoveDnsRequest(payload);
                    sendEvent(context, request);
                }
            }
        };
    }

    @Bean(name = "REMOVE_VAULT_ENTRIES_STATE")
    public Action<?, ?> removeVaultEntriesAction() {
        return new AbstractFreeIpaCleanupAction<>(RemoveDnsResponse.class) {
            @Override
            protected void doExecute(FreeIpaContext context, RemoveDnsResponse payload, Map<Object, Object> variables) {
                if (payload.getHosts() == null || payload.getHosts().isEmpty()) {
                    LOGGER.info("Host is empty, skipping removing vault entries");
                    RemoveVaultEntriesResponse response =
                            new RemoveVaultEntriesResponse(payload, Collections.emptySet(), Collections.emptyMap());
                    sendEvent(context, response);
                } else {
                    RemoveVaultEntriesRequest request = new RemoveVaultEntriesRequest(payload, context.getStack(), context.getFreeIpa());
                    sendEvent(context, request);
                }
            }
        };
    }

    @Bean(name = "REMOVE_USERS_STATE")
    public Action<?, ?> removeUsersAction() {
        return new AbstractFreeIpaCleanupAction<>(RemoveVaultEntriesResponse.class) {
            @Override
            protected void doExecute(FreeIpaContext context, RemoveVaultEntriesResponse payload, Map<Object, Object> variables) {
                if (payload.getUsers() == null || payload.getUsers().isEmpty()) {
                    LOGGER.info("User is empty, skipping removing users");
                    RemoveUsersResponse response =
                            new RemoveUsersResponse(payload, Collections.emptySet(), Collections.emptyMap());
                    sendEvent(context, response);
                } else {
                    RemoveUsersRequest request = new RemoveUsersRequest(payload, context.getStack(), context.getFreeIpa());
                    sendEvent(context, request);
                }
            }
        };
    }

    @Bean(name = "REMOVE_ROLES_STATE")
    public Action<?, ?> removeRolesAction() {
        return new AbstractFreeIpaCleanupAction<>(RemoveUsersResponse.class) {
            @Override
            protected void doExecute(FreeIpaContext context, RemoveUsersResponse payload, Map<Object, Object> variables) {
                if (payload.getRoles() == null || payload.getRoles().isEmpty()) {
                    LOGGER.info("Roles is empty, skipping removing roles");
                    RemoveRolesResponse response =
                            new RemoveRolesResponse(payload, Collections.emptySet(), Collections.emptyMap());
                    sendEvent(context, response);
                } else {
                    RemoveRolesRequest request = new RemoveRolesRequest(payload, context.getStack(), context.getFreeIpa());
                    sendEvent(context, request);
                }
            }
        };
    }

    @Bean(name = "CLEANUP_FINISHED_STATE")
    public Action<?, ?> cleanupFinishedAction() {
        return new AbstractFreeIpaCleanupAction<>(RemoveRolesResponse.class) {

            @Inject
            private OperationStatusService operationStatusService;

            @Override
            protected void doExecute(FreeIpaContext context, RemoveRolesResponse payload, Map<Object, Object> variables) {
                CleanupEvent cleanupEvent = new CleanupEvent(FreeIpaCleanupEvent.CLEANUP_FINISHED_EVENT.event(), payload.getResourceId(), payload.getUsers(),
                        payload.getHosts(), payload.getRoles(), payload.getOperationId(), payload.getClusterName());
                SuccessDetails successDetails = new SuccessDetails(context.getStack().getEnvironmentCrn());
                successDetails.getAdditionalDetails().put("Hosts", payload.getHosts() == null ? List.of() : new ArrayList<>(payload.getHosts()));
                successDetails.getAdditionalDetails().put("Users", payload.getUsers() == null ? List.of() : new ArrayList<>(payload.getUsers()));
                successDetails.getAdditionalDetails().put("Roles", payload.getRoles() == null ? List.of() : new ArrayList<>(payload.getRoles()));
                operationStatusService.completeOperation(payload.getOperationId(), List.of(successDetails), Collections.emptyList());
                LOGGER.info("Cleanup successfully finished with: " + successDetails);
                sendEvent(context, cleanupEvent);
            }
        };
    }

    @Bean(name = "CLEANUP_FAILED_STATE")
    public Action<?, ?> cleanupFailureAction() {
        return new AbstractFreeIpaCleanupAction<>(CleanupFailureEvent.class) {

            @Inject
            private OperationStatusService operationStatusService;

            @Override
            protected void doExecute(FreeIpaContext context, CleanupFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Cleanup failed with payload: " + payload);
                SuccessDetails successDetails = new SuccessDetails(context.getStack().getEnvironmentCrn());
                successDetails.getAdditionalDetails()
                        .put(payload.getFailedPhase(), payload.getSuccess() == null ? List.of() : new ArrayList<>(payload.getSuccess()));
                String message = "Cleanup failed during " + payload.getFailedPhase();
                FailureDetails failureDetails = new FailureDetails(context.getStack().getEnvironmentCrn(), message);
                if (payload.getFailureDetails() != null) {
                    failureDetails.getAdditionalDetails().putAll(payload.getFailureDetails());
                }
                operationStatusService.failOperation(payload.getOperationId(), message, List.of(successDetails), List.of(failureDetails));
                sendEvent(context, FreeIpaCleanupEvent.CLEANUP_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<CleanupFailureEvent>> payloadConverters) {
                payloadConverters.add(new RemoveDnsResponseToCleanupFailureEventConverter());
                payloadConverters.add(new RemoveHostsResponseToCleanupFailureEventConverter());
                payloadConverters.add(new RemoveRolesResponseToCleanupFailureEventConverter());
                payloadConverters.add(new RemoveUsersResponseToCleanupFailureEventConverter());
                payloadConverters.add(new RevokeCertsResponseToCleanupFailureEventConverter());
                payloadConverters.add(new RemoveVaultEntriesResponseToCleanupFailureEventConverter());
            }
        };
    }

}

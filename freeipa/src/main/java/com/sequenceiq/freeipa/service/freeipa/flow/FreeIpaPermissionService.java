package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.Poller;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@Component
public class FreeIpaPermissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPermissionService.class);

    private static final String HOST_ENROLLMENT_PRIVILEGE = "Host Enrollment";

    private static final String ADD_HOSTS_PERMISSION = "System: Add Hosts";

    private static final String REMOVE_SERVICES_PERMISSION = "System: Remove Services";

    private static final String REMOVE_HOSTS_PERMISSION = "System: Remove Hosts";

    private static final String DNS_ADMINISTRATORS_PRIVILEGE = "DNS Administrators";

    private static final String ENROLLMENT_ADMINISTRATOR_ROLE = "Enrollment Administrator";

    @Value("${freeipa.permission.polling.interval}")
    private long pollingInterval;

    @Value("${freeipa.permission.polling.delaymin}")
    private long pollingDelay;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private Poller<Void> poller;

    public void setPermissions(Stack stack, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        List<FreeIpaClient> clientForAllInstances = freeIpaClientFactory.createClientForAllInstances(stack);
        freeIpaClient.addPermissionsToPrivilege(HOST_ENROLLMENT_PRIVILEGE, List.of(ADD_HOSTS_PERMISSION));
        waitForPermissionToReplicate(clientForAllInstances, ADD_HOSTS_PERMISSION);
        freeIpaClient.addPermissionsToPrivilege(HOST_ENROLLMENT_PRIVILEGE, List.of(REMOVE_HOSTS_PERMISSION));
        waitForPermissionToReplicate(clientForAllInstances, REMOVE_HOSTS_PERMISSION);
        freeIpaClient.addPermissionsToPrivilege(HOST_ENROLLMENT_PRIVILEGE, List.of(REMOVE_SERVICES_PERMISSION));
        waitForPermissionToReplicate(clientForAllInstances, REMOVE_SERVICES_PERMISSION);
        freeIpaClient.addRolePrivileges(ENROLLMENT_ADMINISTRATOR_ROLE, Set.of(DNS_ADMINISTRATORS_PRIVILEGE));
    }

    private void waitForPermissionToReplicate(List<FreeIpaClient> clientForAllInstances, String permission) {
        if (!clientForAllInstances.isEmpty()) {
            LOGGER.info("Start polling if [{}] permission is replicated to all instances", permission);
            poller.runPollerDontStopOnException(pollingInterval, pollingDelay,
                    new FreeIpaPermissionReplicatedPoller(clientForAllInstances, HOST_ENROLLMENT_PRIVILEGE, permission));
        } else {
            LOGGER.info("Polling is skipped for non HA FreeIPA deployment");
        }
    }
}

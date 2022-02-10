package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;

@Component
public class FreeIpaPermissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPermissionService.class);

    private static final String HOST_ENROLLMENT_PRIVILEGE = "Host Enrollment";

    private static final String ADD_HOSTS_PERMISSION = "System: Add Hosts";

    private static final String REMOVE_SERVICES_PERMISSION = "System: Remove Services";

    private static final String REMOVE_HOSTS_PERMISSION = "System: Remove Hosts";

    private static final String DNS_ADMINISTRATORS_PRIVILEGE = "DNS Administrators";

    private static final String ENROLLMENT_ADMINISTRATOR_ROLE = "Enrollment Administrator";

    public void setPermissions(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        freeIpaClient.addPermissionsToPrivilege(HOST_ENROLLMENT_PRIVILEGE, List.of(ADD_HOSTS_PERMISSION));
        freeIpaClient.addPermissionsToPrivilege(HOST_ENROLLMENT_PRIVILEGE, List.of(REMOVE_HOSTS_PERMISSION));
        freeIpaClient.addPermissionsToPrivilege(HOST_ENROLLMENT_PRIVILEGE, List.of(REMOVE_SERVICES_PERMISSION));
        freeIpaClient.addRolePrivileges(ENROLLMENT_ADMINISTRATOR_ROLE, Set.of(DNS_ADMINISTRATORS_PRIVILEGE));
    }
}

package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.Privilege;

public class FreeIpaPermissionReplicatedPoller implements AttemptMaker<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPermissionReplicatedPoller.class);

    private final List<FreeIpaClient> clientForAllInstances;

    private final String privilegeName;

    private final String permission;

    private int attempt;

    public FreeIpaPermissionReplicatedPoller(List<FreeIpaClient> clientForAllInstances, String privilege, String permission) {
        this.clientForAllInstances = clientForAllInstances;
        this.privilegeName = privilege;
        this.permission = permission;
    }

    @Override
    public AttemptResult<Void> process() throws Exception {
        attempt++;
        LOGGER.debug("Checking if [{}] permission is replicated for [{}] privilege on all instances. Attempt: [{}]", permission, privilegeName, attempt);
        List<String> replicationMissingForInstance = new ArrayList<>(clientForAllInstances.size());
        for (FreeIpaClient client : clientForAllInstances) {
            Privilege privilege = client.showPrivilege(privilegeName);
            boolean permissionPresent = privilege.getMemberofPermission().contains(permission);
            if (!permissionPresent) {
                replicationMissingForInstance.add(client.getHostname());
            }
        }
        if (replicationMissingForInstance.isEmpty()) {
            LOGGER.info("[{}] permission is replicated for [{}] privilege on all instances", permission, privilegeName);
            return AttemptResults.justFinish();
        } else {
            LOGGER.debug("[{}] permission is missing on {} instances for [{}] privilege", permission, replicationMissingForInstance, privilegeName);
            return AttemptResults.justContinue();
        }
    }
}

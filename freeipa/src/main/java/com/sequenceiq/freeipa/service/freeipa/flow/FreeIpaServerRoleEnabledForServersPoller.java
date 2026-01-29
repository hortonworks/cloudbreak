package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.ServerRole;

public class FreeIpaServerRoleEnabledForServersPoller implements AttemptMaker<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaServerRoleEnabledForServersPoller.class);

    private static final String ENABLED_STATUS = "enabled";

    private final FreeIpaClient freeIpaClient;

    private final String role;

    private final Set<String> servers;

    private int attempt;

    public FreeIpaServerRoleEnabledForServersPoller(FreeIpaClient freeIpaClient, String role, Set<String> servers) {
        Objects.requireNonNull(servers);
        this.freeIpaClient = freeIpaClient;
        this.role = role;
        this.servers = servers;
    }

    @Override
    public AttemptResult<Void> process() throws Exception {
        attempt++;
        LOGGER.debug("Checking if [{}] role is enabled for servers [{}]. Attempt: [{}]", role, servers, attempt);
        try {
            List<ServerRole> serverRoles = freeIpaClient.findServerRoles(role, null, null);
            boolean roleEnabledForAllServers = servers.stream().allMatch(server -> isRoleEnabledForServer(server, serverRoles));
            if (roleEnabledForAllServers) {
                return AttemptResults.justFinish();
            } else {
                LOGGER.debug("Not all servers are enabled for role: {}", serverRoles);
                return AttemptResults.justContinue();
            }
        } catch (FreeIpaClientException e) {
            LOGGER.debug("We were unable to fetch server roles", e);
            return AttemptResults.breakFor(e);
        }
    }

    private boolean isRoleEnabledForServer(String server, List<ServerRole> serverRoles) {
        return serverRoles.stream().anyMatch(serverRole -> serverRole.getServerFqdn().equals(server) && ENABLED_STATUS.equals(serverRole.getStatus()));
    }
}

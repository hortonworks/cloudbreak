package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final Set<String> roles;

    private final Set<String> servers;

    private int attempt;

    public FreeIpaServerRoleEnabledForServersPoller(FreeIpaClient freeIpaClient, Set<String> roles, Set<String> servers) {
        Objects.requireNonNull(roles);
        Objects.requireNonNull(servers);
        this.freeIpaClient = freeIpaClient;
        this.roles = roles;
        this.servers = servers;
    }

    @Override
    public AttemptResult<Void> process() throws Exception {
        attempt++;
        LOGGER.debug("Checking if roles {} are enabled for servers {}. Attempt: [{}]", roles, servers, attempt);
        try {
            List<ServerRole> serverRoles = freeIpaClient.findServerRoles(null, null, null);
            Map<String, Set<String>> missingByRole = new HashMap<>();
            for (String role : roles) {
                Set<String> missing = servers.stream()
                        .filter(server -> !isRoleEnabledForServer(server, role, serverRoles))
                        .collect(Collectors.toSet());
                if (!missing.isEmpty()) {
                    missingByRole.put(role, missing);
                }
            }

            if (missingByRole.isEmpty()) {
                return AttemptResults.justFinish();
            } else {
                String aggregatedError = missingByRole.entrySet().stream()
                        .map(e -> String.format("Role [%s] is not enabled for servers %s", e.getKey(), e.getValue()))
                        .collect(Collectors.joining(", "));
                LOGGER.debug("Not all servers are enabled for all roles: {}", aggregatedError);
                return AttemptResults.continueFor(new FreeIpaClientException(aggregatedError));
            }
        } catch (FreeIpaClientException e) {
            LOGGER.debug("We were unable to fetch server roles", e);
            return AttemptResults.breakFor(e);
        }
    }

    private boolean isRoleEnabledForServer(String server, String role, List<ServerRole> serverRoles) {
        return serverRoles.stream().anyMatch(serverRole ->
                serverRole.getServerFqdn().equals(server)
                        && role.equals(serverRole.getRole())
                        && ENABLED_STATUS.equals(serverRole.getStatus()));
    }
}

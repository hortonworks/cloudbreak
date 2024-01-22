package com.sequenceiq.freeipa.service.freeipa.host;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.IpaServer;
import com.sequenceiq.freeipa.kerberosmgmt.exception.DeleteException;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientRetryService;

@Service
public class HostDeletionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostDeletionService.class);

    private static final String HOST_DELETION_FAILED = "Failed to delete hosts.";

    @Inject
    private FreeIpaClientRetryService retryService;

    @Measure(HostDeletionService.class)
    public Pair<Set<String>, Map<String, String>> removeHosts(FreeIpaClient client, Set<String> hosts) throws FreeIpaClientException {
        LOGGER.info("Removing hosts: {}", hosts);
        return removeHosts(client, hosts, false);
    }

    @Measure(HostDeletionService.class)
    public Pair<Set<String>, Map<String, String>> removeServers(FreeIpaClient client, Set<String> hosts) throws FreeIpaClientException {
        LOGGER.info("Removing servers: {}", hosts);
        return removeHosts(client, hosts, true);
    }

    private Pair<Set<String>, Map<String, String>> removeHosts(FreeIpaClient client, Set<String> hosts, boolean servers) throws FreeIpaClientException {
        Set<String> hostCleanupSuccess = new HashSet<>();
        Map<String, String> hostCleanupFailed = new HashMap<>();
        Set<String> hostsToRemove = collectHostsToRemove(client, hosts, servers);
        LOGGER.debug("Hosts to delete: {}", hostsToRemove);
        for (String host : hostsToRemove) {
            try {
                if (servers) {
                    retryService.retryWhenRetryableWithoutValue(() -> client.deleteServer(host));
                } else {
                    retryService.retryWhenRetryableWithoutValue(() -> client.deleteHost(host));
                }
                hostCleanupSuccess.add(host);
            } catch (FreeIpaClientException e) {
                handleErrorDuringDeletion(hostCleanupSuccess, hostCleanupFailed, host, e);
            }
        }
        return Pair.of(hostCleanupSuccess, hostCleanupFailed);
    }

    private Set<String> collectHostsToRemove(FreeIpaClient client, Set<String> hosts, boolean servers) throws FreeIpaClientException {
        if (hosts.size() > 1) {
            Set<String> existingHostFqdn = collectExistingHostFqdn(client, servers);
            return filterHostsToBeRemoved(hosts, existingHostFqdn);
        } else {
            LOGGER.debug("Skipping filtering and fetching hosts from FreeIPA as for a single host {} it is too much overhead", hosts);
            return hosts;
        }
    }

    private Set<String> collectExistingHostFqdn(FreeIpaClient client, boolean servers) throws FreeIpaClientException {
        if (servers) {
            return collectExistingServersFqdns(client);
        } else {
            return collectExistingHostFqdns(client);
        }
    }

    private void handleErrorDuringDeletion(Set<String> hostCleanupSuccess, Map<String, String> hostCleanupFailed, String host, FreeIpaClientException e) {
        if (FreeIpaClientExceptionUtil.isNotFoundException(e)) {
            hostCleanupSuccess.add(host);
        } else {
            LOGGER.info("Host delete failed for host: {}", host, e);
            hostCleanupFailed.put(host, e.getMessage());
        }
    }

    private Set<String> filterHostsToBeRemoved(Set<String> hosts, Set<String> existingHostFqdn) {
        return hosts.stream()
                .filter(existingHostFqdn::contains)
                .collect(Collectors.toSet());
    }

    private Set<String> collectExistingHostFqdns(FreeIpaClient client) throws FreeIpaClientException {
        return client.findAllHostFqdnOnly().stream().map(Host::getFqdn).collect(Collectors.toSet());
    }

    private Set<String> collectExistingServersFqdns(FreeIpaClient client) throws FreeIpaClientException {
        return client.findAllServers().stream().map(IpaServer::getFqdn).collect(Collectors.toSet());
    }

    public void deleteHostsWithDeleteException(FreeIpaClient client, Set<String> hosts) throws FreeIpaClientException, DeleteException {
        try {
            Pair<Set<String>, Map<String, String>> removeHostsResult = removeHosts(client, hosts);
            if (!removeHostsResult.getSecond().isEmpty()) {
                throw new DeleteException(HOST_DELETION_FAILED + ' ' + removeHostsResult.getSecond());
            }
        } catch (RetryableFreeIpaClientException e) {
            LOGGER.error(HOST_DELETION_FAILED + ' ' + e.getLocalizedMessage(), e);
            throw new RetryableFreeIpaClientException(HOST_DELETION_FAILED, e, new DeleteException(HOST_DELETION_FAILED));
        } catch (FreeIpaClientException e) {
            LOGGER.error(HOST_DELETION_FAILED + ' ' + e.getLocalizedMessage(), e);
            throw new DeleteException(HOST_DELETION_FAILED);
        }
    }
}

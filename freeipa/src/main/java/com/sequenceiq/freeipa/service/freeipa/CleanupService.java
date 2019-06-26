package com.sequenceiq.freeipa.service.freeipa;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZoneList;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.Role;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class CleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupService.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    public CleanupResponse cleanup(String accountId, CleanupRequest request) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(request.getEnvironmentCrn(), accountId);
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        CleanupResponse cleanupResponse = new CleanupResponse();
        if (!CollectionUtils.isEmpty(request.getHosts())) {
            removeHosts(client, request, cleanupResponse);
            removeDnsEntries(client, request, cleanupResponse, freeIpa.getDomain());
        }
        if (!CollectionUtils.isEmpty(request.getUsers())) {
            removeUsers(client, request, cleanupResponse);
        }
        if (!CollectionUtils.isEmpty(request.getRoles())) {
            removeRoles(client, request, cleanupResponse);
        }
        return cleanupResponse;
    }

    private void removeDnsEntries(FreeIpaClient client, CleanupRequest request, CleanupResponse response, String domain) throws FreeIpaClientException {
        Set<String> allDnsZoneName = client.findAllDnsZone().stream().map(DnsZoneList::getIdnsname).collect(Collectors.toSet());
        for (String zone : allDnsZoneName) {
            Set<DnsRecord> allDnsRecordInZone = client.findAllDnsRecordInZone(zone);
            for (String host : request.getHosts()) {
                if (!response.getHostCleanupFailed().containsKey(host)) {
                    allDnsRecordInZone.stream().filter(record -> record.isHostRelatedRecord(host, domain))
                            .forEach(record -> {
                                try {
                                    client.deleteDnsRecord(record.getIdnsname(), zone);
                                    response.getHostCleanupSuccess().add(host);
                                } catch (FreeIpaClientException e) {
                                    LOGGER.info("DNS record delete in zone [{}] with name [{}] failed for host: {}", zone, record.getIdnsname(), host, e);
                                    response.getHostCleanupFailed().put(host, e.getMessage());
                                    response.getHostCleanupSuccess().remove(host);
                                }
                            });
                }
            }
        }
    }

    private void removeHosts(FreeIpaClient client, CleanupRequest request, CleanupResponse response) throws FreeIpaClientException {
        Set<String> existingHostFqdn = client.findAllHost().stream().map(Host::getFqdn).collect(Collectors.toSet());
        Set<String> hostsToRemove = request.getHosts().stream()
                .filter(existingHostFqdn::contains)
                .filter(h -> !response.getHostCleanupFailed().keySet().contains(h))
                .collect(Collectors.toSet());
        for (String host : hostsToRemove) {
            try {
                client.deleteHost(host);
                response.getHostCleanupSuccess().add(host);
            } catch (FreeIpaClientException e) {
                LOGGER.info("Host delete failed for host: {}", host, e);
                response.getHostCleanupFailed().put(host, e.getMessage());
                response.getHostCleanupSuccess().remove(host);
            }
        }
    }

    private void removeUsers(FreeIpaClient client, CleanupRequest request, CleanupResponse response) throws FreeIpaClientException {
        Set<String> usersUid = client.userFindAll().stream().map(User::getUid).collect(Collectors.toSet());
        request.getUsers().stream().filter(usersUid::contains).forEach(userUid -> {
            try {
                client.deleteUser(userUid);
                response.getUserCleanupSuccess().add(userUid);
            } catch (FreeIpaClientException e) {
                LOGGER.info("User delete failed for user: {}", userUid, e);
                response.getUserCleanupFailed().put(userUid, e.getMessage());
                response.getUserCleanupSuccess().remove(userUid);
            }
        });
    }

    private void removeRoles(FreeIpaClient client, CleanupRequest request, CleanupResponse response) throws FreeIpaClientException {
        Set<String> roleNames = client.findAllRole().stream().map(Role::getCn).collect(Collectors.toSet());
        request.getRoles().stream().filter(roleNames::contains).forEach(role -> {
            try {
                client.deleteRole(role);
                response.getRoleCleanupSuccess().add(role);
            } catch (FreeIpaClientException e) {
                LOGGER.info("User delete failed for user: {}", role, e);
                response.getRoleCleanupFailed().put(role, e.getMessage());
                response.getRoleCleanupSuccess().remove(role);
            }
        });
    }
}

package com.sequenceiq.freeipa.service.freeipa;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Cert;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZoneList;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.Role;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberosmgmt.exception.DeleteException;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtV1Controller;
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

    @Inject
    private KerberosMgmtV1Controller kerberosMgmtV1Controller;

    public CleanupResponse cleanup(String accountId, CleanupRequest request) throws FreeIpaClientException {
        Optional<Stack> optionalStack = stackService.findByEnvironmentCrnAndAccountId(request.getEnvironmentCrn(), accountId);
        CleanupResponse cleanupResponse = new CleanupResponse();
        if (optionalStack.isPresent()) {
            Stack stack = optionalStack.get();
            FreeIpa freeIpa = freeIpaService.findByStack(stack);
            FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            if (!CollectionUtils.isEmpty(request.getHosts())) {
                revokeCerts(client, request, cleanupResponse);
                removeHosts(client, request, cleanupResponse);
                removeDnsEntries(client, request, cleanupResponse, freeIpa.getDomain());
                removeVaultEntries(client, request, cleanupResponse, stack.getEnvironmentCrn());
            }
//             TODO: https://jira.cloudera.com/browse/CB-3581
//            if (!CollectionUtils.isEmpty(request.getUsers())) {
//                removeUsers(client, request, cleanupResponse);
//            }
//            if (!CollectionUtils.isEmpty(request.getRoles())) {
//                removeRoles(client, request, cleanupResponse);
//            }
        }
        return cleanupResponse;
    }

    private void revokeCerts(FreeIpaClient client, CleanupRequest request, CleanupResponse cleanupResponse) throws FreeIpaClientException {
        Set<Cert> certs = client.findAllCert();
        certs.stream()
                .filter(cert -> request.getHosts().contains(StringUtils.removeStart(cert.getSubject(), "CN=")))
                .filter(cert -> !cert.isRevoked())
                .forEach(cert -> {
                    try {
                        client.revokeCert(cert.getSerialNumber());
                        cleanupResponse.getCertCleanupSuccess().add(cert.getSubject());
                    } catch (FreeIpaClientException e) {
                        LOGGER.error("Couldn't revoke certificate: {}", cert, e);
                        cleanupResponse.getCertCleanupFailed().put(cert.getSubject(), e.getMessage());
                    }
                });
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
        LOGGER.debug("Hosts to delete: {}", hostsToRemove);
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
        removeHostRelatedServices(client, hostsToRemove);
    }

    private void removeHostRelatedServices(FreeIpaClient client, Set<String> hostsToRemove) throws FreeIpaClientException {
        Map<String, String> principalCanonicalMap = client.findAllService().stream()
                .collect(Collectors.toMap(com.sequenceiq.freeipa.client.model.Service::getKrbprincipalname,
                        com.sequenceiq.freeipa.client.model.Service::getKrbcanonicalname));
        for (String host : hostsToRemove) {
            Set<String> services = principalCanonicalMap.entrySet().stream().filter(e -> e.getKey().contains(host))
                    .map(Map.Entry::getValue).collect(Collectors.toSet());
            LOGGER.debug("Services to delete: {}", services);
            for (String service : services) {
                try {
                    client.deleteService(service);
                } catch (FreeIpaClientException e) {
                    LOGGER.info("Service delete failed for service: {}", service, e);
                }
            }
        }
    }

    private void removeUsers(FreeIpaClient client, CleanupRequest request, CleanupResponse response) throws FreeIpaClientException {
        Set<String> usersUid = client.userFindAll().stream().map(User::getUid).collect(Collectors.toSet());
        request.getUsers().stream().filter(usersUid::contains).forEach(userUid -> {
            try {
                LOGGER.debug("Delete user: {}", userUid);
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
                LOGGER.debug("Delete role: {}", role);
                client.deleteRole(role);
                response.getRoleCleanupSuccess().add(role);
            } catch (FreeIpaClientException e) {
                LOGGER.info("Role delete failed for role: {}", role, e);
                response.getRoleCleanupFailed().put(role, e.getMessage());
                response.getRoleCleanupSuccess().remove(role);
            }
        });
    }

    private void removeVaultEntries(FreeIpaClient client, CleanupRequest request, CleanupResponse response, String environmentCrn) {
        for (String host : request.getHosts()) {
            if (!response.getHostCleanupFailed().containsKey(host)) {
                try {
                    HostRequest hostRequst = new HostRequest();
                    hostRequst.setEnvironmentCrn(environmentCrn);
                    hostRequst.setServerHostName(host);
                    kerberosMgmtV1Controller.deleteHost(hostRequst);
                    response.getHostCleanupSuccess().add(host);
                } catch (DeleteException | FreeIpaClientException e) {
                    LOGGER.info("Vault secret cleanup failed for host: {}", host, e);
                    response.getHostCleanupFailed().put(host, e.getMessage());
                    response.getHostCleanupSuccess().remove(host);
                }
            }
        }
    }

}
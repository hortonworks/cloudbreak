package com.sequenceiq.freeipa.service.freeipa.cleanup;

import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Cert;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZoneList;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.Role;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.kerberosmgmt.exception.DeleteException;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtV1Controller;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationStatusService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class CleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupService.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private StackService stackService;

    @Inject
    private KerberosMgmtV1Controller kerberosMgmtV1Controller;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private OperationStatusService operationStatusService;

    @Inject
    private OperationToOperationStatusConverter operationToOperationStatusConverter;

    @Inject
    private KerberosConfigService kerberosConfigService;

    public OperationStatus cleanup(String accountId, CleanupRequest request) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(request.getEnvironmentCrn(), accountId);
        MDCBuilder.buildMdcContext(stack);
        Operation operation =
                operationStatusService.startOperation(accountId, OperationType.CLEANUP, Set.of(stack.getEnvironmentCrn()), Collections.emptySet());
        CleanupEvent cleanupEvent = new CleanupEvent(FreeIpaCleanupEvent.CLEANUP_EVENT.event(), stack.getId(), request.getUsers(),
                request.getHosts(), request.getRoles(), operation.getOperationId(), request.getClusterName());
        flowManager.notify(FreeIpaCleanupEvent.CLEANUP_EVENT.event(), cleanupEvent);
        return operationToOperationStatusConverter.convert(operation);
    }

    public Pair<Set<String>, Map<String, String>> revokeCerts(Long stackId, Set<String> hosts) throws FreeIpaClientException {
        FreeIpaClient client = getFreeIpaClient(stackId);
        Set<String> certCleanupSuccess = new HashSet<>();
        Map<String, String> certCleanupFailed = new HashMap<>();
        Set<Cert> certs = client.findAllCert();
        certs.stream()
                .filter(cert -> hosts.stream().anyMatch(host -> substringBefore(host, ".").equals(substringBefore(removeStart(cert.getSubject(), "CN="), "."))))
                .filter(cert -> !cert.isRevoked())
                .forEach(cert -> {
                    try {
                        client.revokeCert(cert.getSerialNumber());
                        certCleanupSuccess.add(cert.getSubject());
                    } catch (FreeIpaClientException e) {
                        LOGGER.error("Couldn't revoke certificate: {}", cert, e);
                        certCleanupFailed.put(cert.getSubject(), e.getMessage());
                    }
                });
        return Pair.of(certCleanupSuccess, certCleanupFailed);
    }

    public Pair<Set<String>, Map<String, String>> removeDnsEntries(Long stackId, Set<String> hosts, String domain) throws FreeIpaClientException {
        FreeIpaClient client = getFreeIpaClient(stackId);
        Set<String> dnsCleanupSuccess = new HashSet<>();
        Map<String, String> dnsCleanupFailed = new HashMap<>();
        Set<String> allDnsZoneName = client.findAllDnsZone().stream().map(DnsZoneList::getIdnsname).collect(Collectors.toSet());
        for (String zone : allDnsZoneName) {
            Set<DnsRecord> allDnsRecordInZone = client.findAllDnsRecordInZone(zone);
            for (String host : hosts) {
                allDnsRecordInZone.stream().filter(record -> record.isHostRelatedRecord(host, domain))
                        .forEach(record -> {
                            try {
                                client.deleteDnsRecord(record.getIdnsname(), zone);
                                dnsCleanupSuccess.add(host);
                            } catch (FreeIpaClientException e) {
                                LOGGER.info("DNS record delete in zone [{}] with name [{}] failed for host: {}", zone, record.getIdnsname(), host, e);
                                dnsCleanupFailed.put(host, e.getMessage());
                            }
                        });
            }
        }
        return Pair.of(dnsCleanupSuccess, dnsCleanupFailed);
    }

    public Pair<Set<String>, Map<String, String>> removeHosts(Long stackId, Set<String> hosts) throws FreeIpaClientException {
        FreeIpaClient client = getFreeIpaClient(stackId);
        Set<String> hostCleanupSuccess = new HashSet<>();
        Map<String, String> hostCleanupFailed = new HashMap<>();
        Set<String> existingHostFqdn = client.findAllHost().stream().map(Host::getFqdn).collect(Collectors.toSet());
        Set<String> hostsToRemove = hosts.stream()
                .filter(existingHostFqdn::contains)
                .collect(Collectors.toSet());
        LOGGER.debug("Hosts to delete: {}", hostsToRemove);
        for (String host : hostsToRemove) {
            try {
                client.deleteHost(host);
                hostCleanupSuccess.add(host);
            } catch (FreeIpaClientException e) {
                LOGGER.info("Host delete failed for host: {}", host, e);
                hostCleanupFailed.put(host, e.getMessage());
            }
        }
        removeHostRelatedServices(client, hosts);
        return Pair.of(hostCleanupSuccess, hostCleanupFailed);
    }

    private void removeHostRelatedServices(FreeIpaClient client, Set<String> hostsToRemove) throws FreeIpaClientException {
        Map<String, String> principalCanonicalMap = client.findAllService().stream()
                .collect(Collectors.toMap(com.sequenceiq.freeipa.client.model.Service::getKrbprincipalname,
                        com.sequenceiq.freeipa.client.model.Service::getKrbcanonicalname));
        for (String host : hostsToRemove) {
            Set<String> services = principalCanonicalMap.entrySet().stream().filter(e -> e.getKey().contains(host))
                    .map(Entry::getValue).collect(Collectors.toSet());
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

    public Pair<Set<String>, Map<String, String>> removeUsers(Long stackId, Set<String> users, String clusterName) throws FreeIpaClientException {
        FreeIpaClient client = getFreeIpaClient(stackId);
        Set<String> userCleanupSuccess = new HashSet<>();
        Map<String, String> userCleanupFailed = new HashMap<>();
        Set<String> usersUid = client.userFindAll().stream().map(User::getUid).collect(Collectors.toSet());
        users.stream().filter(usersUid::contains).forEach(userUid -> {
            try {
                LOGGER.debug("Delete user: {}", userUid);
                client.deleteUser(userUid);
                userCleanupSuccess.add(userUid);
            } catch (FreeIpaClientException e) {
                LOGGER.info("User delete failed for user: {}", userUid, e);
                userCleanupFailed.put(userUid, e.getMessage());
            }
        });
        if (StringUtils.isNotBlank(clusterName)) {
            try {
                Stack stack = stackService.getStackById(stackId);
                kerberosConfigService.delete(stack.getEnvironmentCrn(), stack.getAccountId(), clusterName);
            } catch (NotFoundException e) {
                LOGGER.warn("No kerberos config found for cluster [{}] to delete", clusterName);
            }
        }
        return Pair.of(userCleanupSuccess, userCleanupFailed);
    }

    public Pair<Set<String>, Map<String, String>> removeRoles(Long stackId, Set<String> roles) throws FreeIpaClientException {
        FreeIpaClient client = getFreeIpaClient(stackId);
        Set<String> roleCleanupSuccess = new HashSet<>();
        Map<String, String> roleCleanupFailed = new HashMap<>();
        Set<String> roleNames = client.findAllRole().stream().map(Role::getCn).collect(Collectors.toSet());
        roles.stream().filter(roleNames::contains).forEach(role -> {
            try {
                LOGGER.debug("Delete role: {}", role);
                client.deleteRole(role);
                roleCleanupSuccess.add(role);
            } catch (FreeIpaClientException e) {
                LOGGER.info("Role delete failed for role: {}", role, e);
                roleCleanupFailed.put(role, e.getMessage());
            }
        });
        return Pair.of(roleCleanupSuccess, roleCleanupFailed);
    }

    public Pair<Set<String>, Map<String, String>> removeVaultEntries(Long stackId, Set<String> hosts) throws FreeIpaClientException {
        Set<String> vaultCleanupSuccess = new HashSet<>();
        Map<String, String> vaultCleanupFailed = new HashMap<>();
        Stack stack = stackService.getStackById(stackId);
        for (String host : hosts) {
            try {
                HostRequest hostRequest = new HostRequest();
                hostRequest.setEnvironmentCrn(stack.getEnvironmentCrn());
                hostRequest.setServerHostName(host);
                kerberosMgmtV1Controller.deleteHost(hostRequest);
                vaultCleanupSuccess.add(host);
            } catch (DeleteException | FreeIpaClientException e) {
                LOGGER.info("Vault secret cleanup failed for host: {}", host, e);
                vaultCleanupFailed.put(host, e.getMessage());
            }
        }
        return Pair.of(vaultCleanupSuccess, vaultCleanupFailed);
    }

    private FreeIpaClient getFreeIpaClient(Long stackId) throws FreeIpaClientException {
        return freeIpaClientFactory.getFreeIpaClientForStackId(stackId);
    }
}
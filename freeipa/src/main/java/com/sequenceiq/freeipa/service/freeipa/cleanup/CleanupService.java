package com.sequenceiq.freeipa.service.freeipa.cleanup;

import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.model.Cert;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.client.model.Role;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.kerberosmgmt.exception.DeleteException;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtV1Service;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.freeipa.host.HostDeletionService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class CleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupService.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private StackService stackService;

    @Inject
    private KerberosMgmtV1Service kerberosMgmtV1Service;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private OperationService operationService;

    @Inject
    private OperationToOperationStatusConverter operationToOperationStatusConverter;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private HostDeletionService hostDeletionService;

    @Inject
    private CleanupStepToStateNameConverter cleanupStepToStateNameConverter;

    public OperationStatus cleanup(String accountId, CleanupRequest request) {
        String environmentCrn = request.getEnvironmentCrn();
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        Operation operation =
                operationService.startOperation(accountId, OperationType.CLEANUP, Set.of(environmentCrn), Collections.emptySet());
        Set<String> statesToSkip = cleanupStepToStateNameConverter.convert(request.getCleanupStepsToSkip());
        CleanupEvent cleanupEvent = new CleanupEvent(FreeIpaCleanupEvent.CLEANUP_EVENT.event(), stack.getId(), request.getUsers(),
                request.getHosts(), request.getRoles(), request.getIps(), statesToSkip, accountId, operation.getOperationId(),
                request.getClusterName(), environmentCrn);
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

    public Pair<Set<String>, Map<String, String>> removeDnsEntries(Long stackId, Set<String> hosts, Set<String> ips, String domain)
            throws FreeIpaClientException {
        FreeIpaClient client = getFreeIpaClient(stackId);
        Set<String> dnsCleanupSuccess = new HashSet<>();
        Map<String, String> dnsCleanupFailed = new HashMap<>();
        Set<String> allDnsZoneName = client.findAllDnsZone().stream().map(DnsZone::getIdnsname).collect(Collectors.toSet());
        for (String zone : allDnsZoneName) {
            removeHostNameRelatedDnsRecords(hosts, domain, client, dnsCleanupSuccess, dnsCleanupFailed, zone);
            removeIpRelatedRecords(ips, client, dnsCleanupSuccess, dnsCleanupFailed, zone);
        }
        return Pair.of(dnsCleanupSuccess, dnsCleanupFailed);
    }

    private void removeIpRelatedRecords(Set<String> ips, FreeIpaClient client, Set<String> dnsCleanupSuccess, Map<String, String> dnsCleanupFailed,
            String zone) throws FreeIpaClientException {
        if (ips != null && !ips.isEmpty()) {
            Set<DnsRecord> allDnsRecordInZone = client.findAllDnsRecordInZone(zone);
            for (String ip : ips) {
                allDnsRecordInZone.stream().filter(record -> record.isIpRelatedRecord(ip, zone))
                        .forEach(record -> deleteRecord(client, dnsCleanupSuccess, dnsCleanupFailed, zone, ip, record));
            }
        }
    }

    private void removeHostNameRelatedDnsRecords(Set<String> hosts, String domain, FreeIpaClient client, Set<String> dnsCleanupSuccess,
            Map<String, String> dnsCleanupFailed, String zone) throws FreeIpaClientException {
        if (hosts != null && !hosts.isEmpty()) {
            Set<DnsRecord> allDnsRecordInZone = client.findAllDnsRecordInZone(zone);
            for (String host : hosts) {
                allDnsRecordInZone.stream().filter(record -> record.isHostRelatedRecord(host, domain))
                        .forEach(record -> deleteRecord(client, dnsCleanupSuccess, dnsCleanupFailed, zone, host, record));

                allDnsRecordInZone.stream()
                        .filter(record -> record.isHostRelatedSrvRecord(host))
                        .forEach(record -> deleteSrvRecord(client, dnsCleanupSuccess, dnsCleanupFailed, zone, host, record));
            }
        }
    }

    private void deleteRecord(FreeIpaClient client, Set<String> dnsCleanupSuccess, Map<String, String> dnsCleanupFailed, String zone, String host,
            DnsRecord record) {
        try {
            client.deleteDnsRecord(record.getIdnsname(), zone);
            dnsCleanupSuccess.add(host);
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isNotFoundException(e)) {
                dnsCleanupSuccess.add(host);
            } else {
                LOGGER.info("DNS record delete in zone [{}] with name [{}] failed for host: {}", zone, record.getIdnsname(), host, e);
                dnsCleanupFailed.put(host, e.getMessage());
            }
        }
    }

    private void deleteSrvRecord(FreeIpaClient client, Set<String> dnsCleanupSuccess, Map<String, String> dnsCleanupFailed, String zone, String host,
            DnsRecord record) {
        try {
            List<String> srvRecords = record.getHostRelatedSrvRecords(host);
            client.deleteDnsSrvRecord(record.getIdnsname(), zone, srvRecords);
            if (!dnsCleanupFailed.containsKey(host)) {
                dnsCleanupSuccess.add(host);
            }
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isNotFoundException(e)) {
                dnsCleanupSuccess.add(host);
            } else {
                LOGGER.info("DNS record delete in zone [{}] with name [{}] failed for host: {}", zone, record.getIdnsname(), host, e);
                dnsCleanupSuccess.remove(host);
                dnsCleanupFailed.put(host, e.getMessage());
            }
        }
    }

    public Pair<Set<String>, Map<String, String>> removeHosts(Long stackId, Set<String> hosts) throws FreeIpaClientException {
        FreeIpaClient client = getFreeIpaClient(stackId);
        Pair<Set<String>, Map<String, String>> hostDeleteResult = hostDeletionService.removeHosts(client, hosts);
        removeHostRelatedServices(client, hosts);
        return hostDeleteResult;
    }

    public Pair<Set<String>, Map<String, String>> removeServers(Long stackId, Set<String> hosts) throws FreeIpaClientException {
        FreeIpaClient client = getFreeIpaClient(stackId);
        Pair<Set<String>, Map<String, String>> hostDeleteResult = hostDeletionService.removeServers(client, hosts);
        removeHostRelatedServices(client, hosts);
        return hostDeleteResult;
    }

    private void removeHostRelatedServices(FreeIpaClient client, Set<String> hostsToRemove) throws FreeIpaClientException {
        Map<String, String> principalCanonicalMap = createPrincipalCanonicalNameMap(client);
        for (String host : hostsToRemove) {
            Set<String> services = filterForServicesCanonicalNameForDeletion(principalCanonicalMap, host);
            LOGGER.debug("Services to delete: {}", services);
            for (String service : services) {
                try {
                    client.deleteService(service);
                } catch (FreeIpaClientException e) {
                    if (!FreeIpaClientExceptionUtil.isNotFoundException(e)) {
                        LOGGER.info("Service delete failed for service: {}", service, e);
                    }
                }
            }
        }
    }

    private Set<String> filterForServicesCanonicalNameForDeletion(Map<String, String> principalCanonicalMap, String host) {
        return principalCanonicalMap.entrySet().stream().filter(e -> e.getKey().contains(host))
                .map(Entry::getValue).collect(Collectors.toSet());
    }

    private Map<String, String> createPrincipalCanonicalNameMap(FreeIpaClient client) throws FreeIpaClientException {
        return client.findAllService().stream()
                .flatMap(service -> service.getKrbprincipalname().stream()
                        .map(principal -> Map.entry(principal, service.getKrbcanonicalname())))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    public Pair<Set<String>, Map<String, String>> removeUsers(Long stackId, Set<String> users, String clusterName, String environmentCrn)
            throws FreeIpaClientException {
        FreeIpaClient client = getFreeIpaClient(stackId);
        Set<String> userCleanupSuccess = new HashSet<>();
        Map<String, String> userCleanupFailed = new HashMap<>();
        Set<String> usersUid = client.userFindAll().stream().map(User::getUid).collect(Collectors.toSet());
        users.stream().filter(usersUid::contains).forEach(userUid -> {
            try {
                client.deleteUser(userUid);
                userCleanupSuccess.add(userUid);
            } catch (FreeIpaClientException e) {
                if (FreeIpaClientExceptionUtil.isNotFoundException(e)) {
                    userCleanupSuccess.add(userUid);
                } else {
                    LOGGER.info("User delete failed for user: {}", userUid, e);
                    userCleanupFailed.put(userUid, e.getMessage());
                }
            }
        });
        if (StringUtils.isNotBlank(clusterName)) {
            Stack stack = stackService.getStackById(stackId);
            if (StringUtils.isEmpty(environmentCrn)) {
                environmentCrn = stack.getEnvironmentCrn();
            }
            String accountId = stack.getAccountId();
            try {
                kerberosConfigService.delete(environmentCrn, accountId, clusterName);
            } catch (NotFoundException e) {
                LOGGER.warn("No kerberos config found for cluster [{}] to delete", clusterName);
            }
            try {
                ldapConfigService.delete(environmentCrn, accountId, clusterName);
            } catch (NotFoundException e) {
                LOGGER.warn("No ldap config found for cluster [{}] to delete", clusterName);
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
                if (FreeIpaClientExceptionUtil.isNotFoundException(e)) {
                    roleCleanupSuccess.add(role);
                } else {
                    LOGGER.info("Role delete failed for role: {}", role, e);
                    roleCleanupFailed.put(role, e.getMessage());
                }
            }
        });
        return Pair.of(roleCleanupSuccess, roleCleanupFailed);
    }

    public Pair<Set<String>, Map<String, String>> removeVaultEntries(Long stackId, Set<String> hosts) throws FreeIpaClientException {
        Set<String> vaultCleanupSuccess = new HashSet<>();
        Map<String, String> vaultCleanupFailed = new HashMap<>();
        Stack stack = stackService.getStackById(stackId);
        FreeIpaClient freeIpaClient = getFreeIpaClient(stackId);
        for (String host : hosts) {
            try {
                HostRequest hostRequest = new HostRequest();
                hostRequest.setEnvironmentCrn(stack.getEnvironmentCrn());
                hostRequest.setServerHostName(host);
                kerberosMgmtV1Service.removeHostRelatedKerberosConfiguration(hostRequest, stack.getAccountId(), freeIpaClient);
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

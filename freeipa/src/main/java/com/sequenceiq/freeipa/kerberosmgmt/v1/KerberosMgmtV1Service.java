package com.sequenceiq.freeipa.kerberosmgmt.v1;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.RoleRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.VaultCleanupRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.Keytab;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberosmgmt.exception.DeleteException;
import com.sequenceiq.freeipa.kerberosmgmt.exception.KeytabCreationException;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class KerberosMgmtV1Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosMgmtV1Service.class);

    private static final String HOST_CREATION_FAILED = "Failed to create host.";

    private static final String HOST_DELETION_FAILED = "Failed to delete host.";

    private static final String SERVICE_PRINCIPAL_CREATION_FAILED = "Failed to create service principal.";

    private static final String SERVICE_PRINCIPAL_DELETION_FAILED = "Failed to delete service principal.";

    private static final String HOST_ALLOW_FAILURE = "Request to allow the host keytab retrieval failed.";

    private static final String SERVICE_ALLOW_FAILURE = "Request to allow the service to retrieve keytab failed.";

    private static final String KEYTAB_GENERATION_FAILED = "Failed to create keytab.";

    private static final String KEYTAB_FETCH_FAILED = "Failed to fetch keytab.";

    private static final String EMPTY_REALM = "Failed to create service as realm was empty.";

    private static final String ROLE_NOT_ALLOWED = "The role request is not allowed when retrieving a keytab";

    private static final String IPA_STACK_NOT_FOUND = "Stack for IPA server not found.";

    private static final int NOT_FOUND_ERROR_CODE = 4001;

    private static final int DUPLICATE_ENTRY_ERROR_CODE = 4002;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private KerberosMgmtRoleComponent roleComponent;

    @Inject
    private KerberosMgmtVaultComponent vaultComponent;

    public ServiceKeytabResponse generateServiceKeytab(ServiceKeytabRequest request, String accountId) throws FreeIpaClientException {
        LOGGER.debug("Request to generate service keytab: {}", request);
        ServiceKeytabResponse response = new ServiceKeytabResponse();
        Stack freeIpaStack = getFreeIpaStack(request.getEnvironmentCrn(), accountId);
        String realm = getRealm(freeIpaStack);
        FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);
        addHost(request.getServerHostName(), null, ipaClient);
        com.sequenceiq.freeipa.client.model.Service service = serviceAdd(request, realm, ipaClient);
        String serviceKeytab;
        if (service.getHasKeytab() && request.getDoNotRecreateKeytab()) {
            serviceKeytab = getExistingKeytab(service.getKrbprincipalname(), ipaClient);
        } else {
            serviceKeytab = getKeytab(service.getKrbprincipalname(), ipaClient);
        }
        response.setKeytab(vaultComponent.getSecretResponseForKeytab(request, accountId, serviceKeytab));
        response.setServicePrincipal(vaultComponent.getSecretResponseForPrincipal(request, accountId, service.getKrbprincipalname()));
        return response;
    }

    public ServiceKeytabResponse getExistingServiceKeytab(ServiceKeytabRequest request, String accountId) throws FreeIpaClientException {
        LOGGER.debug("Request to get service keytab for account {}: {}", accountId, request);
        if (request.getRoleRequest() != null) {
            throw new KeytabCreationException(ROLE_NOT_ALLOWED);
        }
        ServiceKeytabResponse response = new ServiceKeytabResponse();
        Stack freeIpaStack = getFreeIpaStack(request.getEnvironmentCrn(), accountId);
        String realm = getRealm(freeIpaStack);
        FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);

        String servicePrincipal = request.getServiceName() + "/" + request.getServerHostName() + "@" + realm;
        String serviceKeytab = getExistingKeytab(servicePrincipal, ipaClient);
        response.setKeytab(vaultComponent.getSecretResponseForKeytab(request, accountId, serviceKeytab));
        response.setServicePrincipal(vaultComponent.getSecretResponseForPrincipal(request, accountId, servicePrincipal));
        return response;
    }

    public HostKeytabResponse generateHostKeytab(HostKeytabRequest request, String accountId) throws FreeIpaClientException {
        LOGGER.debug("Request to generate host keytab: {}", request);
        HostKeytabResponse response = new HostKeytabResponse();
        Stack freeIpaStack = getFreeIpaStack(request.getEnvironmentCrn(), accountId);
        FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);
        Host host = addHost(request.getServerHostName(), request.getRoleRequest(), ipaClient);
        String hostKeytab;
        if (host.getHasKeytab() && request.getDoNotRecreateKeytab()) {
            hostKeytab = getExistingKeytab(host.getKrbprincipalname(), ipaClient);
        } else {
            hostKeytab = getKeytab(host.getKrbprincipalname(), ipaClient);
        }
        response.setKeytab(vaultComponent.getSecretResponseForKeytab(request, accountId, hostKeytab));
        response.setHostPrincipal(vaultComponent.getSecretResponseForPrincipal(request, accountId, host.getKrbprincipalname()));
        return response;
    }

    public HostKeytabResponse getExistingHostKeytab(HostKeytabRequest request, String accountId) throws FreeIpaClientException {
        LOGGER.debug("Request to get host keytab for account {}: {}", accountId, request);
        if (request.getRoleRequest() != null) {
            throw new KeytabCreationException(ROLE_NOT_ALLOWED);
        }
        HostKeytabResponse response = new HostKeytabResponse();
        Stack freeIpaStack = getFreeIpaStack(request.getEnvironmentCrn(), accountId);
        FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);

        String hostPrincipal = ipaClient.showHost(request.getServerHostName()).getKrbprincipalname();
        String hostKeytab = getExistingKeytab(hostPrincipal, ipaClient);
        response.setKeytab(vaultComponent.getSecretResponseForKeytab(request, accountId, hostKeytab));
        response.setHostPrincipal(vaultComponent.getSecretResponseForPrincipal(request, accountId, hostPrincipal));
        return response;
    }

    public void deleteServicePrincipal(ServicePrincipalRequest request, String accountId) throws FreeIpaClientException, DeleteException {
        LOGGER.debug("Request to delete service principal for account {}: {}", accountId, request);
        Stack freeIpaStack = getFreeIpaStack(request.getEnvironmentCrn(), accountId);
        String realm = getRealm(freeIpaStack);
        String canonicalPrincipal = constructPrincipal(request.getServiceName(), request.getServerHostName(), realm);
        FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);
        delService(canonicalPrincipal, ipaClient);
        VaultPathBuilder vaultPathBuilder = new VaultPathBuilder()
                .enableGeneratingClusterIdIfNotPresent()
                .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                .withAccountId(accountId)
                .withEnvironmentCrn(request.getEnvironmentCrn())
                .withClusterCrn(request.getClusterCrn())
                .withServerHostName(request.getServerHostName())
                .withServiceName(request.getServiceName());
        vaultComponent.recursivelyCleanupVault(vaultPathBuilder.withSubType(VaultPathBuilder.SecretSubType.SERVICE_PRINCIPAL).build());
        vaultComponent.recursivelyCleanupVault(vaultPathBuilder.withSubType(VaultPathBuilder.SecretSubType.KEYTAB).build());
        roleComponent.deleteRoleIfItIsNoLongerUsed(request.getRoleName(), ipaClient);
    }

    public void deleteHost(HostRequest request, String accountId) throws FreeIpaClientException, DeleteException {
        LOGGER.debug("Request to delete host for account {}: {}", accountId, request);
        Stack freeIpaStack = getFreeIpaStack(request.getEnvironmentCrn(), accountId);
        FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);

        Set<String> services = ipaClient.findAllService().stream()
                .filter(s -> s.getKrbprincipalname().contains(request.getServerHostName()))
                .map(f -> f.getKrbcanonicalname()).collect(Collectors.toSet());
        LOGGER.debug("Services count on the given host: {}", services.size());
        for (String service : services) {
            delService(service, ipaClient);
        }
        delHost(request.getServerHostName(), ipaClient);
        VaultPathBuilder vaultPathBuilder = new VaultPathBuilder()
                .enableGeneratingClusterIdIfNotPresent()
                .withAccountId(accountId)
                .withEnvironmentCrn(request.getEnvironmentCrn())
                .withClusterCrn(request.getClusterCrn())
                .withServerHostName(request.getServerHostName());
        for (VaultPathBuilder.SecretType secretType : VaultPathBuilder.SecretType.values()) {
            vaultPathBuilder.withSecretType(secretType);
            vaultComponent.recursivelyCleanupVault(vaultPathBuilder.withSubType(VaultPathBuilder.SecretSubType.SERVICE_PRINCIPAL).build());
            vaultComponent.recursivelyCleanupVault(vaultPathBuilder.withSubType(VaultPathBuilder.SecretSubType.KEYTAB).build());
        }
        roleComponent.deleteRoleIfItIsNoLongerUsed(request.getRoleName(), ipaClient);
    }

    public void cleanupByCluster(VaultCleanupRequest request, String accountId) throws DeleteException {
        LOGGER.debug("Request to cleanup vault for a cluster for account {}: {}", accountId, request);
        try {
            MDCBuilder.addEnvCrn(request.getEnvironmentCrn());
            MDCBuilder.addAccountId(accountId);
            if (Strings.isNullOrEmpty(request.getClusterCrn())) {
                LOGGER.error("Cluster CRN not provided. Vault is not cleaned-up");
                throw new DeleteException("Cluster CRN is required");
            }
            MDCBuilder.addResourceCrn(request.getClusterCrn());
            vaultComponent.cleanupSecrets(request.getEnvironmentCrn(), request.getClusterCrn(), accountId);
        } catch (DeleteException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Cleanup cluster failed " + e.getLocalizedMessage(), e);
            throw new DeleteException("Failed to cleanup " + e.getLocalizedMessage());
        }
    }

    public void cleanupByEnvironment(String environmentCrn, String accountId) throws DeleteException {
        LOGGER.debug("Request to cleanup vault for an environment for account {}: {}", accountId, environmentCrn);
        try {
            MDCBuilder.addEnvCrn(environmentCrn);
            MDCBuilder.addAccountId(accountId);
            vaultComponent.cleanupSecrets(environmentCrn, null, accountId);
        } catch (Exception e) {
            LOGGER.error("Cleanup cluster failed " + e.getLocalizedMessage(), e);
            throw new DeleteException("Failed to cleanup " + e.getLocalizedMessage());
        }
    }

    private Stack getFreeIpaStack(String envCrn, String accountId) {
        LOGGER.debug("Looking for stack using env:{} and account-id:{}", envCrn, accountId);
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(envCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        return stack;
    }

    private String getRealm(Stack stack) {
        try {
            FreeIpa freeIpa = freeIpaService.findByStack(stack);
            if (!Strings.isNullOrEmpty(freeIpa.getDomain())) {
                LOGGER.debug("Realm of IPA Server: {}", freeIpa.getDomain().toUpperCase());
                return freeIpa.getDomain().toUpperCase();
            }
        } catch (NotFoundException notfound) {
            LOGGER.error("Realm not found");
        }
        throw new KeytabCreationException(EMPTY_REALM);
    }

    private Host addHost(String hostname, RoleRequest roleRequest, FreeIpaClient ipaClient) throws KeytabCreationException {
        Host host;
        try {
            try {
                host = ipaClient.addHost(hostname);
            } catch (FreeIpaClientException e) {
                if (!KerberosMgmtUtil.isDuplicateEntryException(e)) {
                    LOGGER.error(HOST_CREATION_FAILED + " " + e.getLocalizedMessage(), e);
                    throw new KeytabCreationException(HOST_CREATION_FAILED);
                }
                host = ipaClient.showHost(hostname);
            }
            allowHostKeytabRetrieval(hostname, freeIpaClientFactory.getAdminUser(), ipaClient);
            roleComponent.addRoleAndPrivileges(Optional.empty(), Optional.of(host), roleRequest, ipaClient);
        } catch (FreeIpaClientException e) {
            LOGGER.error(HOST_CREATION_FAILED + " " + e.getLocalizedMessage(), e);
            throw new KeytabCreationException(HOST_CREATION_FAILED);
        }
        return host;
    }

    private void delHost(String hostname, FreeIpaClient ipaClient) throws DeleteException {
        try {
            ipaClient.deleteHost(hostname);
        } catch (FreeIpaClientException e) {
            if (!KerberosMgmtUtil.isNotFoundException(e)) {
                LOGGER.error(HOST_DELETION_FAILED + " " + e.getLocalizedMessage(), e);
                throw new DeleteException(HOST_DELETION_FAILED);
            }
        }
    }

    private com.sequenceiq.freeipa.client.model.Service serviceAdd(ServiceKeytabRequest request, String realm, FreeIpaClient ipaClient)
            throws KeytabCreationException {
        String canonicalPrincipal = constructPrincipal(request.getServiceName(), request.getServerHostName(), realm);
        com.sequenceiq.freeipa.client.model.Service service;
        try {
            service = serviceAdd(canonicalPrincipal, ipaClient);
            if (request.getServerHostNameAlias() != null) {
                String aliasPrincipal = constructPrincipal(request.getServiceName(), request.getServerHostNameAlias(), realm);
                if (!aliasPrincipal.equals(canonicalPrincipal)) {
                    service = addServiceAlias(canonicalPrincipal, aliasPrincipal, ipaClient);
                }
            }
            allowServiceKeytabRetrieval(service.getKrbprincipalname(), freeIpaClientFactory.getAdminUser(), ipaClient);
            roleComponent.addRoleAndPrivileges(Optional.of(service), Optional.empty(), request.getRoleRequest(), ipaClient);
        } catch (FreeIpaClientException e) {
            LOGGER.error(SERVICE_PRINCIPAL_CREATION_FAILED + " " + e.getLocalizedMessage(), e);
            throw new KeytabCreationException(SERVICE_PRINCIPAL_CREATION_FAILED);
        }
        return service;
    }

    private com.sequenceiq.freeipa.client.model.Service serviceAdd(String canonicalPrincipal, FreeIpaClient ipaClient) throws FreeIpaClientException {
        com.sequenceiq.freeipa.client.model.Service service;
        try {
            service = ipaClient.addService(canonicalPrincipal);
        } catch (FreeIpaClientException e) {
            if (!KerberosMgmtUtil.isDuplicateEntryException(e)) {
                throw e;
            }
            service = ipaClient.showService(canonicalPrincipal);
        }
        return service;
    }

    private com.sequenceiq.freeipa.client.model.Service addServiceAlias(String canonicalPrincipal, String aliasPrincipal, FreeIpaClient ipaClient)
            throws FreeIpaClientException {
        com.sequenceiq.freeipa.client.model.Service service;
        try {
            service = ipaClient.addServiceAlias(canonicalPrincipal, aliasPrincipal);
        } catch (FreeIpaClientException e) {
            if (!KerberosMgmtUtil.isDuplicateEntryException(e)) {
                throw e;
            }
            service = ipaClient.showService(canonicalPrincipal);
        }
        return service;
    }

    private void delService(String canonicalPrincipal, FreeIpaClient ipaClient) throws DeleteException {
        try {
            ipaClient.deleteService(canonicalPrincipal);
        } catch (FreeIpaClientException e) {
            if (!KerberosMgmtUtil.isNotFoundException(e)) {
                LOGGER.error(SERVICE_PRINCIPAL_DELETION_FAILED + " " + e.getLocalizedMessage(), e);
                throw new DeleteException(SERVICE_PRINCIPAL_DELETION_FAILED);
            }
        }
    }

    private void allowServiceKeytabRetrieval(String canonicalPrincipal, String adminUser, FreeIpaClient ipaClient) throws FreeIpaClientException {
        try {
            ipaClient.allowServiceKeytabRetrieval(canonicalPrincipal, adminUser);
        } catch (FreeIpaClientException e) {
            LOGGER.error(SERVICE_ALLOW_FAILURE + " " + e.getLocalizedMessage(), e);
            throw e;
        }
    }

    private void allowHostKeytabRetrieval(String fqdn, String adminUser, FreeIpaClient ipaClient) throws FreeIpaClientException {
        try {
            ipaClient.allowHostKeytabRetrieval(fqdn, adminUser);
        } catch (FreeIpaClientException e) {
            LOGGER.error(HOST_ALLOW_FAILURE + " " + e.getLocalizedMessage(), e);
            throw e;
        }
    }

    private String getKeytab(String canonicalPrincipal, FreeIpaClient ipaClient) throws KeytabCreationException {
        try {
            Keytab keytab = ipaClient.getKeytab(canonicalPrincipal);
            return keytab.getKeytab();
        } catch (FreeIpaClientException e) {
            LOGGER.error(KEYTAB_GENERATION_FAILED + " " + e.getLocalizedMessage(), e);
            throw new KeytabCreationException(KEYTAB_GENERATION_FAILED);
        }
    }

    private String getExistingKeytab(String canonicalPrincipal, FreeIpaClient ipaClient) throws KeytabCreationException {
        try {
            Keytab keytab = ipaClient.getExistingKeytab(canonicalPrincipal);
            return keytab.getKeytab();
        } catch (FreeIpaClientException e) {
            LOGGER.error(KEYTAB_FETCH_FAILED + " " + e.getLocalizedMessage(), e);
            throw new KeytabCreationException(KEYTAB_FETCH_FAILED);
        }
    }

    private static String constructPrincipal(String serviceName, String hostName, String realm) {
        return serviceName + "/" + hostName + "@" + realm;
    }
}

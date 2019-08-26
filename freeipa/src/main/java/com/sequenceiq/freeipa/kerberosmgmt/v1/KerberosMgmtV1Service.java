package com.sequenceiq.freeipa.kerberosmgmt.v1;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.RoleRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.VaultCleanupRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Keytab;
import com.sequenceiq.freeipa.client.model.Privilege;
import com.sequenceiq.freeipa.client.model.Role;
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

    private static final String SERVICE_ALLOW_FAILURE = "Request to allow the service to retrieve keytab failed.";

    private static final String KEYTAB_GENERATION_FAILED = "Failed to create keytab.";

    private static final String KEYTAB_FETCH_FAILED = "Failed to fetch keytab.";

    private static final String VAULT_UPDATE_FAILED = "Failed to update Vault.";

    private static final String EMPTY_REALM = "Failed to create service as realm was empty.";

    private static final String IPA_STACK_NOT_FOUND = "Stack for IPA server not found.";

    private static final String KEYTAB_SUB_TYPE = "keytab";

    private static final String PRINCIPAL_SUB_TYPE = "serviceprincipal";

    private static final int NOT_FOUND_ERROR_CODE = 4001;

    private static final int DUPLICATE_ENTRY_ERROR_CODE = 4002;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private SecretService secretService;

    @Inject
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    public ServiceKeytabResponse generateServiceKeytab(ServiceKeytabRequest request, String accountId) throws FreeIpaClientException {
        LOGGER.debug("Request to generate keytab: {}", request);
        ServiceKeytabResponse response = new ServiceKeytabResponse();
        Stack freeIpaStack = getFreeIpaStack(request.getEnvironmentCrn(), accountId);
        String realm = getRealm(freeIpaStack);
        FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);
        addHost(request.getServerHostName(), ipaClient);
        com.sequenceiq.freeipa.client.model.Service service = serviceAdd(request, realm, ipaClient);
        String serviceKeytab;
        if (service.getHasKeytab() && request.getDoNotRecreateKeytab()) {
            serviceKeytab = getExistingServiceKeytab(service.getKrbprincipalname(), ipaClient);
        } else {
            serviceKeytab = getServiceKeytab(service.getKrbprincipalname(), ipaClient);
        }
        response.setKeytab(getSecretResponseForKeytab(request, accountId, serviceKeytab));
        response.setServicePrincipal(getSecretResponseForPrincipal(request, accountId, service.getKrbprincipalname()));
        return response;
    }

    public ServiceKeytabResponse getExistingServiceKeytab(ServiceKeytabRequest request, String accountId) throws FreeIpaClientException {
        LOGGER.debug("Request to get keytab for account {}: {}", accountId, request);
        ServiceKeytabResponse response = new ServiceKeytabResponse();
        Stack freeIpaStack = getFreeIpaStack(request.getEnvironmentCrn(), accountId);
        String realm = getRealm(freeIpaStack);
        FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);

        String servicePrincipal = request.getServiceName() + "/" + request.getServerHostName() + "@" + realm;
        String serviceKeytab = getExistingServiceKeytab(servicePrincipal, ipaClient);
        response.setKeytab(getSecretResponseForKeytab(request, accountId, serviceKeytab));
        response.setServicePrincipal(getSecretResponseForPrincipal(request, accountId, servicePrincipal));
        return response;
    }

    public void deleteServicePrincipal(ServicePrincipalRequest request, String accountId) throws FreeIpaClientException, DeleteException {
        LOGGER.debug("Request to delete service principal for account {}: {}", accountId, request);
        Stack freeIpaStack = getFreeIpaStack(request.getEnvironmentCrn(), accountId);
        String realm = getRealm(freeIpaStack);
        String canonicalPrincipal = constructCanonicalPrincipal(request.getServiceName(), request.getServerHostName(), realm);
        FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);
        delService(canonicalPrincipal, ipaClient);
        VaultPathBuilder vaultPathBuilder = new VaultPathBuilder()
                .enableGeneratingClusterIdIfNotPresent()
                .withAccountId(accountId)
                .withEnvironmentCrn(request.getEnvironmentCrn())
                .withClusterCrn(request.getClusterCrn())
                .withServerHostName(request.getServerHostName())
                .withServiceName(request.getServiceName());
        recursivelyCleanupVault(vaultPathBuilder.withSubType(PRINCIPAL_SUB_TYPE).build());
        recursivelyCleanupVault(vaultPathBuilder.withSubType(KEYTAB_SUB_TYPE).build());
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
        recursivelyCleanupVault(vaultPathBuilder.withSubType(PRINCIPAL_SUB_TYPE).build());
        recursivelyCleanupVault(vaultPathBuilder.withSubType(KEYTAB_SUB_TYPE).build());
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
            cleanupSecrets(request.getEnvironmentCrn(), request.getClusterCrn(), accountId);
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
            cleanupSecrets(environmentCrn, null, accountId);
        } catch (Exception e) {
            LOGGER.error("Cleanup cluster failed " + e.getLocalizedMessage(), e);
            throw new DeleteException("Failed to cleanup " + e.getLocalizedMessage());
        }
    }

    private void cleanupSecrets(String environmentCrn, String clusterCrn, String accountId) {
        VaultPathBuilder vaultPathBuilder = new VaultPathBuilder()
                .withAccountId(accountId)
                .withEnvironmentCrn(environmentCrn)
                .withClusterCrn(clusterCrn);
        recursivelyCleanupVault(vaultPathBuilder.withSubType(PRINCIPAL_SUB_TYPE).build());
        recursivelyCleanupVault(vaultPathBuilder.withSubType(KEYTAB_SUB_TYPE).build());
    }

    private void recursivelyCleanupVault(String path) {
        LOGGER.debug("Cleaning vault path: " + path);
        List<String> entries = secretService.listEntries(path);
        if (entries.isEmpty()) {
            secretService.cleanup(path);
        } else {
            entries.stream().forEach(entry -> {
                recursivelyCleanupVault(path + "/" + entry);
            });
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

    private void addHost(String hostname, FreeIpaClient ipaClient) throws KeytabCreationException {
        try {
            ipaClient.addHost(hostname);
        } catch (FreeIpaClientException e) {
            if (!isDuplicateEntryException(e)) {
                LOGGER.error(HOST_CREATION_FAILED + " " + e.getLocalizedMessage(), e);
                throw new KeytabCreationException(HOST_CREATION_FAILED);
            }
        }
    }

    private void delHost(String hostname, FreeIpaClient ipaClient) throws DeleteException {
        try {
            ipaClient.deleteHost(hostname);
        } catch (FreeIpaClientException e) {
            if (!isNotFoundException(e)) {
                LOGGER.error(HOST_DELETION_FAILED + " " + e.getLocalizedMessage(), e);
                throw new DeleteException(HOST_DELETION_FAILED);
            }
        }
    }

    private com.sequenceiq.freeipa.client.model.Service serviceAdd(ServiceKeytabRequest request, String realm, FreeIpaClient ipaClient)
            throws KeytabCreationException {
        String canonicalPrincipal = constructCanonicalPrincipal(request.getServiceName(), request.getServerHostName(), realm);
        com.sequenceiq.freeipa.client.model.Service service;
        try {
            try {
                service = ipaClient.addService(canonicalPrincipal);
            } catch (FreeIpaClientException e) {
                if (!isDuplicateEntryException(e)) {
                    throw e;
                }
                service = ipaClient.showService(canonicalPrincipal);
            }
            allowServiceKeytabRetrieval(service.getKrbprincipalname(), freeIpaClientFactory.getAdminUser(), ipaClient);
            addRoleAndPrivileges(service, request.getRoleRequest(), ipaClient);
        } catch (FreeIpaClientException e) {
            LOGGER.error(SERVICE_PRINCIPAL_CREATION_FAILED + " " + e.getLocalizedMessage(), e);
            throw new KeytabCreationException(SERVICE_PRINCIPAL_CREATION_FAILED);
        }
        return service;
    }

    private void delService(String canonicalPrincipal, FreeIpaClient ipaClient) throws DeleteException {
        try {
            ipaClient.deleteService(canonicalPrincipal);
        } catch (FreeIpaClientException e) {
            if (!isNotFoundException(e)) {
                LOGGER.error(SERVICE_PRINCIPAL_DELETION_FAILED + " " + e.getLocalizedMessage(), e);
                throw new DeleteException(SERVICE_PRINCIPAL_DELETION_FAILED);
            }
        }
    }

    private void addRoleAndPrivileges(com.sequenceiq.freeipa.client.model.Service service, RoleRequest roleRequest, FreeIpaClient ipaClient)
            throws FreeIpaClientException {
        if (roleRequest != null && StringUtils.isNotBlank(roleRequest.getRoleName())) {
            Set<Role> allRole = ipaClient.findAllRole();
            Optional<Role> optionalRole = allRole.stream().filter(role -> role.getCn().equals(roleRequest.getRoleName())).findFirst();
            Role role = optionalRole.isPresent() ? optionalRole.get() : ipaClient.addRole(roleRequest.getRoleName());
            addPrivilegesToRole(roleRequest.getPrivileges(), ipaClient, role);
            role = ipaClient.showRole(role.getCn());
            boolean roleSetForService = service.getMemberOfRole().stream().anyMatch(member -> member.contains(roleRequest.getRoleName()));
            if (!roleSetForService) {
                ipaClient.addRoleMember(role.getCn(), null, null, null, null, Set.of(service.getKrbprincipalname()));
            }
        }
    }

    private void addPrivilegesToRole(Set<String> privileges, FreeIpaClient ipaClient, Role role) throws FreeIpaClientException {
        if (privileges != null) {
            Set<String> privilegesToAdd = privileges.stream().filter(privilegeName -> {
                try {
                    Privilege privilege = ipaClient.showPrivilege(privilegeName);
                    return privilege.getMember().stream().noneMatch(member -> member.equals(role.getCn()));
                } catch (FreeIpaClientException e) {
                    LOGGER.error("Privilege [{}] show error", privilegeName, e);
                    return false;
                }
            }).collect(Collectors.toSet());
            if (!privilegesToAdd.isEmpty()) {
                ipaClient.addRolePriviliges(role.getCn(), privilegesToAdd);
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

    private String getServiceKeytab(String canonicalPrincipal, FreeIpaClient ipaClient) throws KeytabCreationException {
        try {
            Keytab keytab = ipaClient.getKeytab(canonicalPrincipal);
            return keytab.getKeytab();
        } catch (FreeIpaClientException e) {
            LOGGER.error(KEYTAB_GENERATION_FAILED + " " + e.getLocalizedMessage(), e);
            throw new KeytabCreationException(KEYTAB_GENERATION_FAILED);
        }
    }

    private String getExistingServiceKeytab(String canonicalPrincipal, FreeIpaClient ipaClient) throws KeytabCreationException {
        try {
            Keytab keytab = ipaClient.getExistingKeytab(canonicalPrincipal);
            return keytab.getKeytab();
        } catch (FreeIpaClientException e) {
            LOGGER.error(KEYTAB_FETCH_FAILED + " " + e.getLocalizedMessage(), e);
            throw new KeytabCreationException(KEYTAB_FETCH_FAILED);
        }
    }

    private boolean isNotFoundException(FreeIpaClientException e) {
        return Optional.ofNullable(e.getCause())
                .filter(JsonRpcClientException.class::isInstance)
                .map(JsonRpcClientException.class::cast)
                .map(JsonRpcClientException::getCode)
                .filter(c -> c == NOT_FOUND_ERROR_CODE)
                .isPresent();
    }

    private boolean isDuplicateEntryException(FreeIpaClientException e) {
        return Optional.ofNullable(e.getCause())
                .filter(JsonRpcClientException.class::isInstance)
                .map(JsonRpcClientException.class::cast)
                .map(JsonRpcClientException::getCode)
                .filter(c -> c == DUPLICATE_ENTRY_ERROR_CODE)
                .isPresent();
    }

    private SecretResponse getSecretResponseForPrincipal(ServiceKeytabRequest request, String accountId, String principal) {
        try {
            String path = new VaultPathBuilder()
                    .enableGeneratingClusterIdIfNotPresent()
                    .withAccountId(accountId)
                    .withSubType(PRINCIPAL_SUB_TYPE)
                    .withEnvironmentCrn(request.getEnvironmentCrn())
                    .withClusterCrn(request.getClusterCrn())
                    .withServerHostName(request.getServerHostName())
                    .withServiceName(request.getServiceName())
                    .build();
            String secret = secretService.put(path, principal);
            return stringToSecretResponseConverter.convert(secret);
        } catch (Exception exception) {
            LOGGER.warn("Failure while updating vault.", exception);
            throw new KeytabCreationException(VAULT_UPDATE_FAILED);
        }
    }

    private SecretResponse getSecretResponseForKeytab(ServiceKeytabRequest request, String accountId, String keytab) {
        try {
            String path = new VaultPathBuilder()
                    .enableGeneratingClusterIdIfNotPresent()
                    .withAccountId(accountId)
                    .withSubType(KEYTAB_SUB_TYPE)
                    .withEnvironmentCrn(request.getEnvironmentCrn())
                    .withClusterCrn(request.getClusterCrn())
                    .withServerHostName(request.getServerHostName())
                    .withServiceName(request.getServiceName())
                    .build();
            String secret = secretService.put(path, keytab);
            return stringToSecretResponseConverter.convert(secret);
        } catch (Exception exception) {
            LOGGER.warn("Failure while updating vault.", exception);
            throw new KeytabCreationException(VAULT_UPDATE_FAILED);
        }
    }

    private static String constructCanonicalPrincipal(String serviceName, String hostName, String realm) {
        return serviceName + "/" + hostName + "@" + realm;
    }

}

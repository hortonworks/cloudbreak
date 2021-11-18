package com.sequenceiq.freeipa.kerberosmgmt.v1;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.VaultCleanupRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberosmgmt.exception.DeleteException;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.host.HostDeletionService;

@Service
public class KeytabCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeytabCleanupService.class);

    private static final String SERVICE_PRINCIPAL_DELETION_FAILED = "Failed to delete service principal.";

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private KerberosMgmtRoleComponent roleComponent;

    @Inject
    private KerberosMgmtVaultComponent vaultComponent;

    @Inject
    private HostDeletionService hostDeletionService;

    @Inject
    private KeytabCacheService keytabCacheService;

    @Inject
    private KeytabCommonService keytabCommonService;

    public void deleteServicePrincipal(ServicePrincipalRequest request, String accountId) throws FreeIpaClientException, DeleteException {
        LOGGER.debug("Request to delete service principal for account {}: {}", accountId, request);
        Stack freeIpaStack = keytabCommonService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), accountId);
        String realm = keytabCommonService.getRealm(freeIpaStack);
        String canonicalPrincipal = keytabCommonService.constructPrincipal(request.getServiceName(), request.getServerHostName(), realm);
        FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);
        deleteService(canonicalPrincipal, ipaClient);
        VaultPathBuilder vaultPathBuilder = new VaultPathBuilder()
                .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                .withAccountId(accountId)
                .withEnvironmentCrn(request.getEnvironmentCrn())
                .withClusterCrn(request.getClusterCrn())
                .withServerHostName(request.getServerHostName())
                .withServiceName(request.getServiceName());
        vaultComponent.recursivelyCleanupVault(vaultPathBuilder.withSubType(VaultPathBuilder.SecretSubType.SERVICE_PRINCIPAL).build());
        vaultComponent.recursivelyCleanupVault(vaultPathBuilder.withSubType(VaultPathBuilder.SecretSubType.KEYTAB).build());
        roleComponent.deleteRoleIfItIsNoLongerUsed(request.getRoleName(), ipaClient);
        keytabCacheService.deleteByEnvironmentCrnAndPrincipal(request.getEnvironmentCrn(), canonicalPrincipal);
    }

    public void deleteHost(HostRequest request, String accountId) throws FreeIpaClientException {
        FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientByAccountAndEnvironment(request.getEnvironmentCrn(), accountId);
        cleanupServicesForHost(request, accountId, ipaClient);
        hostDeletionService.deleteHostsWithDeleteException(ipaClient, Set.of(request.getServerHostName()));
        cleanupVaultAndRolesForHost(request, accountId, ipaClient);
    }

    public void removeHostRelatedKerberosConfiguration(HostRequest request, String accountId, FreeIpaClient ipaClient) throws FreeIpaClientException {
        cleanupServicesForHost(request, accountId, ipaClient);
        cleanupVaultAndRolesForHost(request, accountId, ipaClient);
    }

    private void cleanupVaultAndRolesForHost(HostRequest request, String accountId, FreeIpaClient ipaClient) throws FreeIpaClientException {
        VaultPathBuilder vaultPathBuilder = new VaultPathBuilder()
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

    private void cleanupServicesForHost(HostRequest request, String accountId, FreeIpaClient ipaClient) throws FreeIpaClientException {
        LOGGER.debug("Request to delete host for account {}: {}", accountId, request);

        Set<String> services = ipaClient.findAllService().stream()
                .map(com.sequenceiq.freeipa.client.model.Service::getKrbcanonicalname)
                .filter(krbcanonicalname -> krbcanonicalname.contains(request.getServerHostName()))
                .collect(Collectors.toSet());
        LOGGER.debug("Services count on the given host: {}", services.size());
        for (String service : services) {
            deleteService(service, ipaClient);
            keytabCacheService.deleteByEnvironmentCrnAndPrincipal(request.getEnvironmentCrn(), service);
        }
    }

    public void cleanupByCluster(VaultCleanupRequest request, String accountId) throws DeleteException {
        LOGGER.debug("Request to cleanup vault for a cluster for account {}: {}", accountId, request);
        try {
            MDCBuilder.addEnvCrn(request.getEnvironmentCrn());
            MDCBuilder.addAccountId(accountId);
            if (Strings.isNullOrEmpty(request.getClusterCrn())) {
                LOGGER.error("Cluster CRN not provided. Vault is not cleaned-up");
                throw new DeleteException("Cluster CRN is required");
            } else {
                MDCBuilder.addResourceCrn(request.getClusterCrn());
                vaultComponent.cleanupSecrets(request.getEnvironmentCrn(), request.getClusterCrn(), accountId);
            }
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
            keytabCacheService.deleteByEnvironmentCrn(environmentCrn);
        } catch (Exception e) {
            LOGGER.error("Cleanup cluster failed " + e.getLocalizedMessage(), e);
            throw new DeleteException("Failed to cleanup " + e.getLocalizedMessage());
        }
    }

    private void deleteService(String canonicalPrincipal, FreeIpaClient ipaClient) throws FreeIpaClientException, DeleteException {
        try {
            FreeIpaClientExceptionUtil.ignoreNotFoundException(() -> ipaClient.deleteService(canonicalPrincipal), null);
        } catch (RetryableFreeIpaClientException e) {
            LOGGER.error(SERVICE_PRINCIPAL_DELETION_FAILED + " " + e.getLocalizedMessage(), e);
            throw new RetryableFreeIpaClientException(SERVICE_PRINCIPAL_DELETION_FAILED, e, new DeleteException(SERVICE_PRINCIPAL_DELETION_FAILED));
        } catch (FreeIpaClientException e) {
            LOGGER.error(SERVICE_PRINCIPAL_DELETION_FAILED + " " + e.getLocalizedMessage(), e);
            throw new DeleteException(SERVICE_PRINCIPAL_DELETION_FAILED);
        }
    }
}

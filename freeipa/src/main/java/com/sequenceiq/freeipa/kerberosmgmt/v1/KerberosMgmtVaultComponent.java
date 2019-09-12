package com.sequenceiq.freeipa.kerberosmgmt.v1;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.kerberosmgmt.exception.KeytabCreationException;

@Component
public class KerberosMgmtVaultComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosMgmtV1Service.class);

    private static final String VAULT_UPDATE_FAILED = "Failed to update Vault.";

    @Inject
    private SecretService secretService;

    @Inject
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    public void cleanupSecrets(String environmentCrn, String clusterCrn, String accountId) {
        VaultPathBuilder vaultPathBuilder = new VaultPathBuilder()
                .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                .withAccountId(accountId)
                .withEnvironmentCrn(environmentCrn)
                .withClusterCrn(clusterCrn);
        for (VaultPathBuilder.SecretType secretType : VaultPathBuilder.SecretType.values()) {
            vaultPathBuilder.withSecretType(secretType);
            recursivelyCleanupVault(vaultPathBuilder.withSubType(VaultPathBuilder.SecretSubType.SERVICE_PRINCIPAL).build());
            recursivelyCleanupVault(vaultPathBuilder.withSubType(VaultPathBuilder.SecretSubType.KEYTAB).build());
        }
    }

    public void recursivelyCleanupVault(String path) {
        LOGGER.debug("Cleaning vault path: " + path);
        List<String> entries = secretService.listEntries(path);
        if (entries.isEmpty()) {
            secretService.cleanup(path);
        } else {
            String pathWithTrailingSlash = path.endsWith("/") ? path : path + "/";
            entries.stream().forEach(entry -> {
                recursivelyCleanupVault(pathWithTrailingSlash + entry);
            });
        }
    }

    public SecretResponse getSecretResponseForPrincipal(ServiceKeytabRequest request, String accountId, String principal) {
        try {
            String path = new VaultPathBuilder()
                    .enableGeneratingClusterIdIfNotPresent()
                    .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                    .withAccountId(accountId)
                    .withSubType(VaultPathBuilder.SecretSubType.SERVICE_PRINCIPAL)
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

    public SecretResponse getSecretResponseForKeytab(ServiceKeytabRequest request, String accountId, String keytab) {
        try {
            String path = new VaultPathBuilder()
                    .enableGeneratingClusterIdIfNotPresent()
                    .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                    .withAccountId(accountId)
                    .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
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

    public SecretResponse getSecretResponseForPrincipal(HostKeytabRequest request, String accountId, String principal) {
        try {
            String path = new VaultPathBuilder()
                    .enableGeneratingClusterIdIfNotPresent()
                    .withSecretType(VaultPathBuilder.SecretType.HOST_KEYTAB)
                    .withAccountId(accountId)
                    .withSubType(VaultPathBuilder.SecretSubType.SERVICE_PRINCIPAL)
                    .withEnvironmentCrn(request.getEnvironmentCrn())
                    .withClusterCrn(request.getClusterCrn())
                    .withServerHostName(request.getServerHostName())
                    .build();
            String secret = secretService.put(path, principal);
            return stringToSecretResponseConverter.convert(secret);
        } catch (Exception exception) {
            LOGGER.warn("Failure while updating vault.", exception);
            throw new KeytabCreationException(VAULT_UPDATE_FAILED);
        }
    }

    public SecretResponse getSecretResponseForKeytab(HostKeytabRequest request, String accountId, String keytab) {
        try {
            String path = new VaultPathBuilder()
                    .enableGeneratingClusterIdIfNotPresent()
                    .withSecretType(VaultPathBuilder.SecretType.HOST_KEYTAB)
                    .withAccountId(accountId)
                    .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                    .withEnvironmentCrn(request.getEnvironmentCrn())
                    .withClusterCrn(request.getClusterCrn())
                    .withServerHostName(request.getServerHostName())
                    .build();
            String secret = secretService.put(path, keytab);
            return stringToSecretResponseConverter.convert(secret);
        } catch (Exception exception) {
            LOGGER.warn("Failure while updating vault.", exception);
            throw new KeytabCreationException(VAULT_UPDATE_FAILED);
        }
    }

}

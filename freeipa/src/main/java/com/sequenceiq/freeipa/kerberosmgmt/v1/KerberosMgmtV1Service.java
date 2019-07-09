package com.sequenceiq.freeipa.kerberosmgmt.v1;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Keytab;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberosmgmt.exception.KeytabCreationException;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class KerberosMgmtV1Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosMgmtV1Service.class);

    private static final String HOST_CREATION_FAILED = "Failed to create host.";

    private static final String SERVICE_PRINCIPAL_CREATION_FAILED = "Failed to create service principal.";

    private static final String SERVICE_ALLOW_FAILURE = "Request to allow the service to retrieve keytab failed.";

    private static final String KEYTAB_GENERATION_FAILED = "Failed to create keytab.";

    private static final String KEYTAB_FETCH_FAILED = "Failed to fetch keytab.";

    private static final String VAULT_UPDATE_FAILED = "Failed to update Vault.";

    private static final String EMPTY_REALM = "Failed to create service as realm was empty.";

    private static final String IPA_STACK_NOT_FOUND = "Stack for IPA server not found.";

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
        LOGGER.debug("Request to generate keytab for Service:{} Host:{} in Environment:{}", request.getServiceName(), request.getServerHostName(),
                request.getEnvironmentCrn());
        ServiceKeytabResponse response = new ServiceKeytabResponse();
        FreeIpaClient ipaClient;
        Stack freeIpaStack = getFreeIpaStack(request.getEnvironmentCrn(), accountId);
        String realm = getRealm(freeIpaStack);
        ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);
        String adminUser = freeIpaClientFactory.getAdminUser();
        hostAdd(request.getServerHostName(), ipaClient);
        String servicePrincipal = serviceAdd(request.getServiceName(), request.getServerHostName(), realm, adminUser, ipaClient);
        String serviceKeytab = getServiceKeytab(servicePrincipal, ipaClient);
        response.setKeytab(getSecretResponseForKeytab(accountId, serviceKeytab));
        response.setServicePrincial(getSecretResponseForPrincipal(accountId, servicePrincipal));
        return response;
    }

    public ServiceKeytabResponse getExistingServiceKeytab(ServiceKeytabRequest request, String accountId) throws FreeIpaClientException {
        ServiceKeytabResponse response = new ServiceKeytabResponse();
        FreeIpaClient ipaClient;
        Stack freeIpaStack = getFreeIpaStack(request.getEnvironmentCrn(), accountId);
        String realm = getRealm(freeIpaStack);
        ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);

        String servicePrincipal = request.getServiceName() + "/" + request.getServerHostName() + "@" + realm;
        String serviceKeytab = getExistingServiceKeytab(servicePrincipal, ipaClient);
        response.setKeytab(getSecretResponseForKeytab(accountId, serviceKeytab));
        response.setServicePrincial(getSecretResponseForPrincipal(accountId, servicePrincipal));
        return response;
    }

    public void deleteServicePrincipal(ServicePrincipalRequest request, String accountId) {
        //TODO  Will be implemented CDPSDX-515
        return;
    }

    public void deleteHost(HostRequest request, String accountId) {
        //TODO  Will be implemented CDPSDX-515
        return;
    }

    private Stack getFreeIpaStack(String envCrn, String accountId) {
        LOGGER.debug("Looking for stack using env:{} and account-id:{}", envCrn, accountId);
        return stackService.getByEnvironmentCrnAndAccountId(envCrn, accountId);
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

    private void hostAdd(String hostname, FreeIpaClient ipaClient) throws KeytabCreationException {
        try {
            ipaClient.addHost(hostname);
        } catch (FreeIpaClientException e) {
            if (!isDuplicateEntryException(e)) {
                LOGGER.error(HOST_CREATION_FAILED + " " + e.getLocalizedMessage(), e);
                throw new KeytabCreationException(HOST_CREATION_FAILED);
            }
        }
    }

    private String serviceAdd(String serviceName, String hostname, String realm, String adminUser, FreeIpaClient ipaClient) throws KeytabCreationException {
        String canonicalPrincipal = serviceName + "/" + hostname + "@" + realm;
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
            allowServiceKeytabRetrieval(service.getKrbprincipalname(), adminUser, ipaClient);
        } catch (FreeIpaClientException e) {
            LOGGER.error(SERVICE_PRINCIPAL_CREATION_FAILED + " " + e.getLocalizedMessage(), e);
            throw new KeytabCreationException(SERVICE_PRINCIPAL_CREATION_FAILED);
        }
        return service.getKrbprincipalname();
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

    private boolean isDuplicateEntryException(FreeIpaClientException e) {
        return Optional.ofNullable(e.getCause())
                .filter(JsonRpcClientException.class::isInstance)
                .map(JsonRpcClientException.class::cast)
                .map(JsonRpcClientException::getCode)
                .filter(c -> c == DUPLICATE_ENTRY_ERROR_CODE)
                .isPresent();
    }

    private SecretResponse getSecretResponseForPrincipal(String accountId, String principal) {
        try {
            String path = constructVaultPath(accountId, "ServiceKeytab", "serviceprincipal");
            String secret = secretService.put(path, principal);
            return stringToSecretResponseConverter.convert(secret);
        } catch (Exception exception) {
            LOGGER.warn("Failure while updating vault.", exception);
            throw new KeytabCreationException(VAULT_UPDATE_FAILED);
        }
    }

    private SecretResponse getSecretResponseForKeytab(String accountId, String keytab) {
        try {
            String path = constructVaultPath(accountId, "ServiceKeytab", "keytab");
            String secret = secretService.put(path, keytab);
            return stringToSecretResponseConverter.convert(secret);
        } catch (Exception exception) {
            LOGGER.warn("Failure while updating vault.", exception);
            throw new KeytabCreationException(VAULT_UPDATE_FAILED);
        }
    }

    private String constructVaultPath(String accountId, String type, String subtype) {
        return String.format("%s/%s/%s/%s-%s", accountId, type, subtype,
                UUID.randomUUID().toString(), Long.toHexString(System.currentTimeMillis()));
    }
}

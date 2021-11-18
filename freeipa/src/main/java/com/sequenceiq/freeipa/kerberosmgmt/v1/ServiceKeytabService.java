package com.sequenceiq.freeipa.kerberosmgmt.v1;

import static com.sequenceiq.freeipa.client.FreeIpaErrorCodes.DUPLICATE_ENTRY;
import static com.sequenceiq.freeipa.client.FreeIpaErrorCodes.EXECUTION_ERROR;
import static com.sequenceiq.freeipa.kerberosmgmt.v1.KeytabCommonService.PRIVILEGE_DOES_NOT_EXIST;
import static com.sequenceiq.freeipa.kerberosmgmt.v1.KeytabCommonService.ROLE_NOT_ALLOWED;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.entity.KeytabCache;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberosmgmt.exception.KeytabCreationException;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@Service
public class ServiceKeytabService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceKeytabService.class);

    private static final String SERVICE_PRINCIPAL_CREATION_FAILED = "Failed to create service principal.";

    private static final String SERVICE_ALLOW_FAILURE = "Request to allow the service to retrieve keytab failed.";

    @Inject
    private KeytabCommonService keytabCommonService;

    @Inject
    private StringToSecretResponseConverter secretResponseConverter;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private KerberosMgmtRoleComponent roleComponent;

    @Inject
    private KeytabCacheService keytabCacheService;

    public ServiceKeytabResponse generateServiceKeytab(ServiceKeytabRequest request, String accountId) throws FreeIpaClientException {
        LOGGER.debug("Request to generate service keytab: {}", request);
        Stack freeIpaStack = keytabCommonService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), accountId);
        String realm = keytabCommonService.getRealm(freeIpaStack);
        String principal = keytabCommonService.constructPrincipal(request.getServiceName(), request.getServerHostName(), realm);
        Optional<KeytabCache> keytabCache = keytabCacheService.findByEnvironmentCrnAndPrincipal(request.getEnvironmentCrn(), principal);
        if (request.getDoNotRecreateKeytab() && keytabCache.isPresent()) {
            LOGGER.debug("Keytab is found in cache, using it");
            return createServiceKeytabResponse(keytabCache.get());
        } else {
            LOGGER.debug("Keytab is not found in cache, or existing can't be reused.");
            FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);
            if (!roleComponent.privilegesExist(request.getRoleRequest(), ipaClient)) {
                throw new KeytabCreationException(PRIVILEGE_DOES_NOT_EXIST);
            }
            keytabCommonService.addHost(request.getServerHostName(), null, ipaClient);
            com.sequenceiq.freeipa.client.model.Service service = addAndSetupService(request, realm, ipaClient);
            KeytabCache serviceKeytab = fetchKeytabFromFreeIpa(request, ipaClient, service);
            return createServiceKeytabResponse(serviceKeytab);
        }
    }

    private KeytabCache fetchKeytabFromFreeIpa(ServiceKeytabRequest request, FreeIpaClient ipaClient, com.sequenceiq.freeipa.client.model.Service service)
            throws FreeIpaClientException {
        if (service.getHasKeytab() && request.getDoNotRecreateKeytab()) {
            LOGGER.debug("Service [{}] already has a keytab, and DoNotRecreateKeytab flag is true", service.getKrbcanonicalname());
            return keytabCommonService.getExistingKeytab(request.getEnvironmentCrn(), service.getKrbcanonicalname(), request.getServerHostName(), ipaClient);
        } else {
            LOGGER.debug("Service [{}] has keytab state: [{}] DoNotRecreateKeytab flag is [{}]",
                    service.getKrbcanonicalname(), service.getHasKeytab(), request.getDoNotRecreateKeytab());
            return keytabCommonService.getKeytab(request.getEnvironmentCrn(), service.getKrbcanonicalname(), request.getServerHostName(), ipaClient);
        }
    }

    public ServiceKeytabResponse getExistingServiceKeytab(ServiceKeytabRequest request, String accountId) throws FreeIpaClientException {
        LOGGER.debug("Request to get service keytab for account {}: {}", accountId, request);
        validateRoleRequestNotPresent(request);
        Stack freeIpaStack = keytabCommonService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), accountId);
        String realm = keytabCommonService.getRealm(freeIpaStack);
        String servicePrincipal = keytabCommonService.constructPrincipal(request.getServiceName(), request.getServerHostName(), realm);
        Optional<KeytabCache> keytabCacheOptional = keytabCacheService.findByEnvironmentCrnAndPrincipal(request.getEnvironmentCrn(), servicePrincipal);
        if (keytabCacheOptional.isPresent()) {
            LOGGER.debug("Keytab is found in cache, using it");
            return createServiceKeytabResponse(keytabCacheOptional.get());
        } else {
            LOGGER.debug("Keytab is not found in cache.");
            FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);
            KeytabCache serviceKeytab = keytabCommonService.getExistingKeytab(request.getEnvironmentCrn(), servicePrincipal, request.getServerHostName(),
                    ipaClient);
            return createServiceKeytabResponse(serviceKeytab);
        }
    }

    private void validateRoleRequestNotPresent(ServiceKeytabRequest request) {
        if (request.getRoleRequest() != null) {
            LOGGER.info("Modifying roles when retrieving existing keytab is not possible: {}", request);
            throw new KeytabCreationException(ROLE_NOT_ALLOWED);
        }
    }

    private ServiceKeytabResponse createServiceKeytabResponse(KeytabCache serviceKeytab) {
        ServiceKeytabResponse response = new ServiceKeytabResponse();
        response.setKeytab(secretResponseConverter.convert(serviceKeytab.getKeytab().getSecret()));
        response.setServicePrincipal(secretResponseConverter.convert(serviceKeytab.getPrincipal().getSecret()));
        return response;
    }

    private com.sequenceiq.freeipa.client.model.Service addAndSetupService(ServiceKeytabRequest request, String realm, FreeIpaClient ipaClient)
            throws FreeIpaClientException, KeytabCreationException {
        String canonicalPrincipal = keytabCommonService.constructPrincipal(request.getServiceName(), request.getServerHostName(), realm);
        try {
            com.sequenceiq.freeipa.client.model.Service service = createOrGetService(canonicalPrincipal, ipaClient);
            addAliasToService(request, realm, ipaClient, canonicalPrincipal, service);
            allowServiceKeytabRetrieval(service.getKrbcanonicalname(), ipaClient);
            roleComponent.addRoleAndPrivileges(Optional.of(service), Optional.empty(), request.getRoleRequest(), ipaClient);
            return service;
        } catch (RetryableFreeIpaClientException e) {
            LOGGER.error(SERVICE_PRINCIPAL_CREATION_FAILED + ' ' + e.getLocalizedMessage(), e);
            throw new RetryableFreeIpaClientException(SERVICE_PRINCIPAL_CREATION_FAILED, e, new KeytabCreationException(SERVICE_PRINCIPAL_CREATION_FAILED));
        } catch (FreeIpaClientException e) {
            LOGGER.error(SERVICE_PRINCIPAL_CREATION_FAILED + ' ' + e.getLocalizedMessage(), e);
            throw new KeytabCreationException(SERVICE_PRINCIPAL_CREATION_FAILED);
        }
    }

    private void addAliasToService(ServiceKeytabRequest request, String realm, FreeIpaClient ipaClient, String canonicalPrincipal,
            com.sequenceiq.freeipa.client.model.Service service) throws FreeIpaClientException {
        if (request.getServerHostNameAlias() != null) {
            String aliasPrincipal = keytabCommonService.constructPrincipal(request.getServiceName(), request.getServerHostNameAlias(), realm);
            boolean aliasAlreadyExists = service.getKrbprincipalname().stream().anyMatch(aliasPrincipal::equals);
            if (!aliasAlreadyExists) {
                addServiceAlias(canonicalPrincipal, aliasPrincipal, ipaClient);
            }
        }
    }

    private com.sequenceiq.freeipa.client.model.Service createOrGetService(String canonicalPrincipal, FreeIpaClient ipaClient) throws FreeIpaClientException {
        try {
            Optional<com.sequenceiq.freeipa.client.model.Service> optionalService = fetchServiceIfAlreadyExist(canonicalPrincipal, ipaClient);
            return optionalService.isEmpty() ? ipaClient.addService(canonicalPrincipal) : optionalService.get();
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isDuplicateEntryException(e)) {
                return ipaClient.showService(canonicalPrincipal);
            } else {
                throw e;
            }
        }
    }

    private Optional<com.sequenceiq.freeipa.client.model.Service> fetchServiceIfAlreadyExist(String canonicalPrincipal, FreeIpaClient ipaClient)
            throws FreeIpaClientException {
        return FreeIpaClientExceptionUtil.ignoreNotFoundExceptionWithValue(() -> ipaClient.showService(canonicalPrincipal),
                "Service not found for principal: [{}]", canonicalPrincipal);
    }

    private void addServiceAlias(String canonicalPrincipal, String aliasPrincipal, FreeIpaClient ipaClient)
            throws FreeIpaClientException {
        try {
            ipaClient.addServiceAlias(canonicalPrincipal, aliasPrincipal);
        } catch (FreeIpaClientException e) {
            LOGGER.warn("Adding alias [{}] for [{}] failed", aliasPrincipal, canonicalPrincipal, e);
            if (FreeIpaClientExceptionUtil.isExceptionWithErrorCode(e,
                    Set.of(EXECUTION_ERROR, DUPLICATE_ENTRY))) {
                ipaClient.showService(canonicalPrincipal);
            } else {
                throw e;
            }
        }
    }

    private void allowServiceKeytabRetrieval(String canonicalPrincipal, FreeIpaClient ipaClient) throws FreeIpaClientException {
        try {
            ipaClient.allowServiceKeytabRetrieval(canonicalPrincipal, FreeIpaClientFactory.ADMIN_USER);
        } catch (FreeIpaClientException e) {
            LOGGER.error(SERVICE_ALLOW_FAILURE + " " + e.getLocalizedMessage(), e);
            throw e;
        }
    }
}

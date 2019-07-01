package com.sequenceiq.freeipa.kerberosmgmt.v1;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Keytab;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberosmgmt.exception.KeytabCreationException;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class KerberosMgmtV1Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosMgmtV1Service.class);

    private static final String HOST_CREATION_FAILED = "Failed to create host.";

    private static final String SERVICE_PRINCIPAL_CREATION_FAILED = "Failed to create service principal.";

    private static final String SERVICE_ALLOW_FAILURE = "Request to allow the service to retrieve keytab failed.";

    private static final String KEYTAB_GENERATION_FAILED = "Failed to create keytab.";

    private static final String KEYTAB_FETCH_FAILED = "Failed to fetch keytab.";

    private static final String EMPTY_REALM = "Failed to create service as realm was empty.";

    private static final String IPA_STACK_NOT_FOUND = "Stack for IPA server not found.";

    @Inject
    private CrnService crnService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    public ServiceKeytabResponse generateServiceKeytab(ServiceKeytabRequest request, String accountId) throws FreeIpaClientException {
        LOGGER.debug("Request to generate keytab for Service:{} Host:{} in Environment:{}", request.getServiceName(), request.getServerHostName(),
                request.getEnvironmentCrn());
        ServiceKeytabResponse response = new ServiceKeytabResponse();
        FreeIpaClient ipaClient;
        Stack freeIpaStack = getFreeIpaStack(request.getEnvironmentCrn(), accountId);
        String realm = getRealm(freeIpaStack);
        ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);
        if (!hostAdd(request.getServerHostName(), ipaClient)) {
            throw new KeytabCreationException(HOST_CREATION_FAILED);
        }
        String servicePrincipal = serviceAdd(request.getServiceName(), request.getServerHostName(), realm, ipaClient);
        if (Strings.isNullOrEmpty(servicePrincipal)) {
            throw new KeytabCreationException(SERVICE_PRINCIPAL_CREATION_FAILED);
        }
        String serviceKaytab = getServiceKeytab(servicePrincipal, ipaClient);
        if (Strings.isNullOrEmpty(serviceKaytab)) {
            throw new KeytabCreationException(KEYTAB_GENERATION_FAILED);
        }
        response.setKeytab(serviceKaytab);
        response.setServicePrincial(servicePrincipal);
        return response;
    }

    public ServiceKeytabResponse getExistingServiceKeytab(ServiceKeytabRequest request, String accountId) throws FreeIpaClientException {
        ServiceKeytabResponse response = new ServiceKeytabResponse();
        FreeIpaClient ipaClient;
        Stack freeIpaStack = getFreeIpaStack(request.getEnvironmentCrn(), accountId);
        String realm = getRealm(freeIpaStack);
        ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);

        String servicePrincipal = request.getServiceName() + "/" + request.getServerHostName() + "@" + realm;
        String serviceKaytab = getExistingServiceKeytab(servicePrincipal, ipaClient);
        if (Strings.isNullOrEmpty(serviceKaytab)) {
            throw new KeytabCreationException(KEYTAB_FETCH_FAILED);
        }
        response.setKeytab(serviceKaytab);
        response.setServicePrincial(servicePrincipal);
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

    private boolean hostAdd(String hostname, FreeIpaClient ipaClient) {
        try {
            RPCResponse<com.sequenceiq.freeipa.client.model.Host> response;
            response = ipaClient.addHost(hostname);
            if (response == null || response.getResult() == null) {
                return false;
            }
            // TODO Handle Failure cases.
        } catch (FreeIpaClientException e) {
            LOGGER.error(HOST_CREATION_FAILED + e.getMessage());
            return false;
        }
        return true;
    }

    private String serviceAdd(String serviceName, String hostname, String realm, FreeIpaClient ipaClient) {
        RPCResponse<com.sequenceiq.freeipa.client.model.Service> response;
        com.sequenceiq.freeipa.client.model.Service service;
        try {
            response = ipaClient.addService(serviceName + "/" + hostname + "@" + realm);
            if (response == null || response.getResult() == null) {
                return null;
            }
            service = (com.sequenceiq.freeipa.client.model.Service) response.getResult();
            serviceAllowRetrieveKeytab(service.getKrbprincipalname(), ipaClient);
            // TODO Handle Failure cases.
        } catch (FreeIpaClientException e) {
            LOGGER.error(SERVICE_PRINCIPAL_CREATION_FAILED + e.getMessage());
            return null;
        }
        return service.getKrbprincipalname();
    }

    private String serviceAllowRetrieveKeytab(String canonicalPrincipal, FreeIpaClient ipaClient) {
        RPCResponse<com.sequenceiq.freeipa.client.model.Service> response;
        com.sequenceiq.freeipa.client.model.Service service;
        try {
            response = ipaClient.serviceAllowRetrieveKeytab(canonicalPrincipal, crnService.getCurrentUserId());
            // TODO Handle Failure cases.
        } catch (FreeIpaClientException e) {
            LOGGER.error(SERVICE_ALLOW_FAILURE + e.getMessage());
            return null;
        }
        service = (com.sequenceiq.freeipa.client.model.Service) response.getResult();
        return service.getKrbprincipalname();
    }

    private String getServiceKeytab(String canonicalPrincipal, FreeIpaClient ipaClient) {
        RPCResponse<Keytab> response;
        Keytab keytab;
        try {
            response = ipaClient.getKeytab(canonicalPrincipal);
            if (response == null || response.getResult() == null) {
                return null;
            }
            // TODO Handle Failure cases.
        } catch (FreeIpaClientException e) {
            LOGGER.error(KEYTAB_GENERATION_FAILED + e.getMessage());
            return null;
        }
        keytab = (Keytab) response.getResult();
        return keytab.getKeytab();
    }

    private String getExistingServiceKeytab(String canonicalPrincipal, FreeIpaClient ipaClient) {
        RPCResponse<Keytab> response;
        Keytab keytab;
        try {
            response = ipaClient.getExistingKeytab(canonicalPrincipal);
            if (response == null || response.getResult() == null) {
                return null;
            }
            // TODO Handle Failure cases.
        } catch (FreeIpaClientException e) {
            LOGGER.error(KEYTAB_FETCH_FAILED + e.getMessage());
            return null;
        }
        keytab = (Keytab) response.getResult();
        return keytab.getKeytab();
    }
}

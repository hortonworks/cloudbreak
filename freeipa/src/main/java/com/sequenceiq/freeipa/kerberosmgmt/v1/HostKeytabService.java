package com.sequenceiq.freeipa.kerberosmgmt.v1;

import static com.sequenceiq.freeipa.kerberosmgmt.v1.KeytabCommonService.PRIVILEGE_DOES_NOT_EXIST;
import static com.sequenceiq.freeipa.kerberosmgmt.v1.KeytabCommonService.ROLE_NOT_ALLOWED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.entity.KeytabCache;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@Service
public class HostKeytabService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostKeytabService.class);

    @Inject
    private KeytabCommonService keytabCommonService;

    @Inject
    private StringToSecretResponseConverter secretResponseConverter;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private KerberosMgmtRoleComponent roleComponent;

    public HostKeytabResponse generateHostKeytab(HostKeytabRequest request, String accountId) throws FreeIpaClientException {
        LOGGER.debug("Request to generate host keytab: {}", request);
        Stack freeIpaStack = keytabCommonService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), accountId);
        FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);
        if (!roleComponent.privilegesExist(request.getRoleRequest(), ipaClient)) {
            throw new BadRequestException(PRIVILEGE_DOES_NOT_EXIST);
        } else {
            Host host = keytabCommonService.addHost(request.getServerHostName(), request.getRoleRequest(), ipaClient);
            KeytabCache hostKeytab = fetchKeytab(request, ipaClient, host);
            return createHostKeytabResponse(hostKeytab);
        }
    }

    private KeytabCache fetchKeytab(HostKeytabRequest request, FreeIpaClient ipaClient, Host host) throws FreeIpaClientException {
        if (host.getHasKeytab() && request.getDoNotRecreateKeytab()) {
            return keytabCommonService.getExistingKeytab(request.getEnvironmentCrn(), host.getKrbprincipalname(), request.getServerHostName(), ipaClient);
        } else {
            return keytabCommonService.getKeytab(request.getEnvironmentCrn(), host.getKrbprincipalname(), request.getServerHostName(), ipaClient);
        }
    }

    private HostKeytabResponse createHostKeytabResponse(KeytabCache hostKeytab) {
        HostKeytabResponse response = new HostKeytabResponse();
        response.setKeytab(secretResponseConverter.convert(hostKeytab.getKeytab().getSecret()));
        response.setHostPrincipal(secretResponseConverter.convert(hostKeytab.getPrincipal().getSecret()));
        return response;
    }

    public HostKeytabResponse getExistingHostKeytab(HostKeytabRequest request, String accountId) throws FreeIpaClientException {
        LOGGER.debug("Request to get host keytab for account {}: {}", accountId, request);
        if (request.getRoleRequest() != null) {
            throw new BadRequestException(ROLE_NOT_ALLOWED);
        } else {
            Stack freeIpaStack = keytabCommonService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), accountId);
            FreeIpaClient ipaClient = freeIpaClientFactory.getFreeIpaClientForStack(freeIpaStack);
            String hostPrincipal = ipaClient.showHost(request.getServerHostName()).getKrbprincipalname();
            KeytabCache hostKeytab = keytabCommonService.getExistingKeytab(request.getEnvironmentCrn(), hostPrincipal, request.getServerHostName(), ipaClient);
            return createHostKeytabResponse(hostKeytab);
        }
    }

}

package com.sequenceiq.freeipa.kerberosmgmt.v1;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.KerberosMgmtV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.VaultCleanupRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.kerberosmgmt.exception.DeleteException;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
@Transactional(Transactional.TxType.NEVER)
@AuthorizationResource(type = AuthorizationResourceType.ENVIRONMENT)
public class KerberosMgmtV1Controller implements KerberosMgmtV1Endpoint {
    @Inject
    private KerberosMgmtV1Service kerberosMgmtV1Service;

    @Inject
    private CrnService crnService;

    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public ServiceKeytabResponse generateServiceKeytab(@Valid ServiceKeytabRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return kerberosMgmtV1Service.generateServiceKeytab(request, accountId);
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public ServiceKeytabResponse getServiceKeytab(@Valid ServiceKeytabRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return kerberosMgmtV1Service.getExistingServiceKeytab(request, accountId);
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public HostKeytabResponse generateHostKeytab(@Valid HostKeytabRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return kerberosMgmtV1Service.generateHostKeytab(request, accountId);
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public HostKeytabResponse getHostKeytab(@Valid HostKeytabRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return kerberosMgmtV1Service.getExistingHostKeytab(request, accountId);
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void deleteServicePrincipal(@Valid ServicePrincipalRequest request) throws FreeIpaClientException, DeleteException {
        String accountId = crnService.getCurrentAccountId();
        kerberosMgmtV1Service.deleteServicePrincipal(request, accountId);
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void deleteHost(@Valid HostRequest request) throws FreeIpaClientException, DeleteException {
        String accountId = crnService.getCurrentAccountId();
        kerberosMgmtV1Service.deleteHost(request, accountId);
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void cleanupClusterSecrets(@Valid VaultCleanupRequest request) throws DeleteException {
        String accountId = crnService.getCurrentAccountId();
        kerberosMgmtV1Service.cleanupByCluster(request, accountId);
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void cleanupEnvironmentSecrets(String environmentCrn) throws DeleteException {
        String accountId = crnService.getCurrentAccountId();
        kerberosMgmtV1Service.cleanupByEnvironment(environmentCrn, accountId);
    }
}

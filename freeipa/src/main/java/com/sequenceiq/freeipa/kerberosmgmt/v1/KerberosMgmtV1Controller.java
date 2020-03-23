package com.sequenceiq.freeipa.kerberosmgmt.v1;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.sequenceiq.authorization.service.UmsAuthorizationService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosMgmtV1Controller.class);

    private static final String GET_USER_KEYTAB_RIGHT = "environments/getKeytab";

    @Inject
    private KerberosMgmtV1Service kerberosMgmtV1Service;

    @Inject
    private CrnService crnService;

    @Inject
    private UserKeytabService userKeytabService;

    @Inject
    private UmsAuthorizationService umsAuthorizationService;

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

    @Override
    public String getUserKeytab(@NotEmpty String environmentCrn, @NotEmpty String targetUserCrn) {
        String actorCrn = checkActorCrn();
        LOGGER.debug("getUserKeytab() request for environmentCrn={} for targetUserCrn={} as actorCrn={}",
                environmentCrn, actorCrn, targetUserCrn);
        umsAuthorizationService.checkCallerIsSelfOrHasRight(actorCrn, targetUserCrn, AuthorizationResourceType.ENVIRONMENT,
                AuthorizationResourceAction.GET_KEYTAB);
        return userKeytabService.getKeytabBase64(targetUserCrn, environmentCrn);
    }

    private String checkActorCrn() {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (userCrn == null) {
            throw new BadRequestException("An actor CRN must be provided");
        }
        return userCrn;
    }

}

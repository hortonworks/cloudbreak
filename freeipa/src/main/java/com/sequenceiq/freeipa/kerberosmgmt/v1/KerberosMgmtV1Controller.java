package com.sequenceiq.freeipa.kerberosmgmt.v1;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.UmsAccountAuthorizationService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.util.CheckedFunction;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.KerberosMgmtV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.VaultCleanupRequest;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientService;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.kerberosmgmt.exception.DeleteException;
import com.sequenceiq.freeipa.kerberosmgmt.exception.KeytabCreationException;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
@Transactional(Transactional.TxType.NEVER)
@AuthorizationResource
public class KerberosMgmtV1Controller implements KerberosMgmtV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosMgmtV1Controller.class);

    private static final String GET_USER_KEYTAB_RIGHT = "environments/getKeytab";

    private static final String KEYTAB_CREATION_FAILED = "Keytab creation failed.";

    private static final String DELETION_FAILED = "Deletion failed.";

    @Inject
    private KerberosMgmtV1Service kerberosMgmtV1Service;

    @Inject
    private CrnService crnService;

    @Inject
    private UserKeytabService userKeytabService;

    @Inject
    private UmsAccountAuthorizationService umsAccountAuthorizationService;

    @Inject
    private RetryableFreeIpaClientService retryableFreeIpaClientService;

    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_WRITE)
    public ServiceKeytabResponse generateServiceKeytab(@Valid ServiceKeytabRequest request) throws KeytabCreationException {
        return retryableWithKeytabCreationException((Void v) -> {
            String accountId = crnService.getCurrentAccountId();
            return kerberosMgmtV1Service.generateServiceKeytab(request, accountId);
        });
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    public ServiceKeytabResponse getServiceKeytab(@Valid ServiceKeytabRequest request) throws KeytabCreationException {
        return retryableWithKeytabCreationException((Void v) -> {
            String accountId = crnService.getCurrentAccountId();
            return kerberosMgmtV1Service.getExistingServiceKeytab(request, accountId);
        });
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_WRITE)
    public HostKeytabResponse generateHostKeytab(@Valid HostKeytabRequest request) throws KeytabCreationException {
        return retryableWithKeytabCreationException((Void v) -> {
            String accountId = crnService.getCurrentAccountId();
            return kerberosMgmtV1Service.generateHostKeytab(request, accountId);
        });
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    public HostKeytabResponse getHostKeytab(@Valid HostKeytabRequest request) throws KeytabCreationException {
        return retryableWithKeytabCreationException((Void v) -> {
            String accountId = crnService.getCurrentAccountId();
            return kerberosMgmtV1Service.getExistingHostKeytab(request, accountId);
        });
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_WRITE)
    public void deleteServicePrincipal(@Valid ServicePrincipalRequest request) throws DeleteException {
        retryableWithDeletionException((Void v) -> {
            String accountId = crnService.getCurrentAccountId();
            kerberosMgmtV1Service.deleteServicePrincipal(request, accountId);
            return null;
        });
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_WRITE)
    public void deleteHost(@Valid HostRequest request) throws DeleteException {
        retryableWithDeletionException((Void v) -> {
            String accountId = crnService.getCurrentAccountId();
            kerberosMgmtV1Service.deleteHost(request, accountId);
            return null;
        });
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_WRITE)
    public void cleanupClusterSecrets(@Valid VaultCleanupRequest request) throws DeleteException {
        String accountId = crnService.getCurrentAccountId();
        kerberosMgmtV1Service.cleanupByCluster(request, accountId);
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_WRITE)
    public void cleanupEnvironmentSecrets(String environmentCrn) throws DeleteException {
        String accountId = crnService.getCurrentAccountId();
        kerberosMgmtV1Service.cleanupByEnvironment(environmentCrn, accountId);
    }

    @DisableCheckPermissions
    public String getUserKeytab(@NotEmpty String environmentCrn, @NotEmpty String targetUserCrn) {
        String actorCrn = checkActorCrn();
        LOGGER.debug("getUserKeytab() request for environmentCrn={} for targetUserCrn={} as actorCrn={}",
                environmentCrn, actorCrn, targetUserCrn);
        umsAccountAuthorizationService.checkCallerIsSelfOrHasRight(actorCrn, targetUserCrn, AuthorizationResourceAction.GET_KEYTAB);
        return userKeytabService.getKeytabBase64(targetUserCrn, environmentCrn);
    }

    private String checkActorCrn() {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (userCrn == null) {
            throw new BadRequestException("An actor CRN must be provided");
        }
        return userCrn;
    }

    private <T> T retryableWithKeytabCreationException(CheckedFunction<Void, T, Exception> f) throws KeytabCreationException {
        try {
            return retryableFreeIpaClientService.invokeWithRetries(f);
        } catch (KeytabCreationException e) {
            throw e;
        } catch (RetryableFreeIpaClientException e) {
            LOGGER.error("internal server error", e);
            Exception exceptionForRestApi = e.getExceptionForRestApi();
            if (exceptionForRestApi instanceof KeytabCreationException) {
                throw (KeytabCreationException) exceptionForRestApi;
            }
            throw new KeytabCreationException(KEYTAB_CREATION_FAILED);
        } catch (Exception e2) {
            LOGGER.error("internal server error", e2);
            throw new KeytabCreationException(KEYTAB_CREATION_FAILED);
        }
    }

    private <T> T retryableWithDeletionException(CheckedFunction<Void, T, Exception> f) throws DeleteException {
        try {
            return retryableFreeIpaClientService.invokeWithRetries(f);
        } catch (DeleteException e) {
            throw e;
        } catch (RetryableFreeIpaClientException e) {
            LOGGER.error("internal server error", e);
            Exception exceptionForRestApi = e.getExceptionForRestApi();
            if (exceptionForRestApi instanceof DeleteException) {
                throw (DeleteException) exceptionForRestApi;
            }
            throw new DeleteException(DELETION_FAILED);
        } catch (Exception e2) {
            LOGGER.error("internal server error", e2);
            throw new DeleteException(DELETION_FAILED);
        }
    }
}

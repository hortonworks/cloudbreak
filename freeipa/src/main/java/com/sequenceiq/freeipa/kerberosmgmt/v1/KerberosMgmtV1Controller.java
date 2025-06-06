package com.sequenceiq.freeipa.kerberosmgmt.v1;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CustomCheckUtil;
import com.sequenceiq.authorization.service.UmsAccountAuthorizationService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.util.CheckedFunction;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.KerberosMgmtV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.VaultCleanupRequest;
import com.sequenceiq.freeipa.client.InvalidFreeIpaStateException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientService;
import com.sequenceiq.freeipa.kerberosmgmt.exception.DeleteException;
import com.sequenceiq.freeipa.kerberosmgmt.exception.KeytabCreationException;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class KerberosMgmtV1Controller implements KerberosMgmtV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosMgmtV1Controller.class);

    private static final String KEYTAB_CREATION_FAILED = "Keytab creation failed.";

    private static final String DELETION_FAILED = "Deletion failed. Reason: ";

    @Inject
    private KeytabCleanupService keytabCleanupService;

    @Inject
    private ServiceKeytabService serviceKeytab;

    @Inject
    private HostKeytabService hostKeytab;

    @Inject
    private CrnService crnService;

    @Inject
    private UserKeytabService userKeytabService;

    @Inject
    private UmsAccountAuthorizationService umsAccountAuthorizationService;

    @Inject
    private RetryableFreeIpaClientService retryableFreeIpaClientService;

    @Inject
    private CustomCheckUtil customCheckUtil;

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public ServiceKeytabResponse generateServiceKeytab(@RequestObject ServiceKeytabRequest request, @AccountId String accountIdForInternalUsage) {
        return retryableWithKeytabCreationException((Void v) -> {
            String accountId = crnService.getCurrentAccountId();
            return serviceKeytab.generateServiceKeytab(request, accountId);
        });
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public ServiceKeytabResponse getServiceKeytab(@RequestObject ServiceKeytabRequest request) {
        return retryableWithKeytabCreationException((Void v) -> {
            String accountId = crnService.getCurrentAccountId();
            return serviceKeytab.getExistingServiceKeytab(request, accountId);
        });
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public HostKeytabResponse generateHostKeytab(@RequestObject HostKeytabRequest request) {
        return retryableWithKeytabCreationException((Void v) -> {
            String accountId = crnService.getCurrentAccountId();
            return hostKeytab.generateHostKeytab(request, accountId);
        });
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public HostKeytabResponse getHostKeytab(@RequestObject HostKeytabRequest request) {
        return retryableWithKeytabCreationException((Void v) -> {
            String accountId = crnService.getCurrentAccountId();
            return hostKeytab.getExistingHostKeytab(request, accountId);
        });
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public void deleteServicePrincipal(@RequestObject ServicePrincipalRequest request) {
        retryableWithDeletionException((Void v) -> {
            String accountId = crnService.getCurrentAccountId();
            keytabCleanupService.deleteServicePrincipal(request, accountId);
            return null;
        });
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public void deleteHost(@RequestObject HostRequest request) {
        retryableWithDeletionException((Void v) -> {
            String accountId = crnService.getCurrentAccountId();
            keytabCleanupService.deleteHost(request, accountId);
            return null;
        });
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public void cleanupClusterSecrets(@RequestObject VaultCleanupRequest request) {
        String accountId = crnService.getCurrentAccountId();
        keytabCleanupService.cleanupByCluster(request, accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public void cleanupEnvironmentSecrets(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        keytabCleanupService.cleanupByEnvironment(environmentCrn, accountId);
    }

    @Override
    @CustomPermissionCheck
    public String getUserKeytab(String environmentCrn, String targetUserCrn) {
        String actorCrn = checkActorCrn();
        LOGGER.debug("getUserKeytab() request for environmentCrn={} for targetUserCrn={} as actorCrn={}",
                environmentCrn, actorCrn, targetUserCrn);
        customCheckUtil.run(() -> umsAccountAuthorizationService.
                checkCallerIsSelfOrHasRight(actorCrn, targetUserCrn, AuthorizationResourceAction.GET_KEYTAB));
        return userKeytabService.getKeytabBase64(targetUserCrn, environmentCrn);
    }

    private String checkActorCrn() {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (userCrn == null) {
            throw new BadRequestException("An actor CRN must be provided");
        }
        return userCrn;
    }

    private <T> T retryableWithKeytabCreationException(CheckedFunction<Void, T, Exception> f) {
        try {
            return retryableFreeIpaClientService.invokeWithRetries(f);
        } catch (KeytabCreationException | InvalidFreeIpaStateException | NotFoundException e) {
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

    private <T> T retryableWithDeletionException(CheckedFunction<Void, T, Exception> f) {
        try {
            return retryableFreeIpaClientService.invokeWithRetries(f);
        } catch (DeleteException | InvalidFreeIpaStateException | NotFoundException e) {
            throw e;
        } catch (RetryableFreeIpaClientException e) {
            LOGGER.error("internal server error", e);
            Exception exceptionForRestApi = e.getExceptionForRestApi();
            if (exceptionForRestApi instanceof DeleteException) {
                throw (DeleteException) exceptionForRestApi;
            }
            String reason = exceptionForRestApi == null ? e.getMessage() : exceptionForRestApi.getMessage();
            throw new DeleteException(DELETION_FAILED + reason);
        } catch (Exception e2) {
            LOGGER.error("internal server error", e2);
            throw new DeleteException(DELETION_FAILED + e2.getMessage());
        }
    }
}

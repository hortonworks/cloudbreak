package com.sequenceiq.freeipa.ldap.v1;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareCrnParam;
import com.sequenceiq.freeipa.api.v1.ldap.LdapConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.ldap.model.create.CreateLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigResponse;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.util.CrnService;
import com.sequenceiq.notification.NotificationController;

@Controller
@Transactional(TxType.NEVER)
@InternalReady
@AuthorizationResource
public class LdapConfigV1Controller extends NotificationController implements LdapConfigV1Endpoint {
    @Inject
    private LdapConfigV1Service ldapConfigV1Service;

    @Inject
    private CrnService crnService;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public DescribeLdapConfigResponse describe(@ResourceCrn String environmentCrn) {
        return ldapConfigV1Service.describe(environmentCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public DescribeLdapConfigResponse getForCluster(@ResourceCrn @TenantAwareCrnParam String environmentCrn, String clusterName) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return ldapConfigV1Service.getForCluster(environmentCrn, accountId, clusterName);
    }

    @Override
    @CheckPermissionByResourceObject
    public DescribeLdapConfigResponse create(@ResourceObject CreateLdapConfigRequest request) {
        return ldapConfigV1Service.post(request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public void delete(@ResourceCrn String environmentCrn) {
        ldapConfigV1Service.delete(environmentCrn);
    }

    @Override
    @CheckPermissionByResourceObject
    public TestLdapConfigResponse test(@ResourceObject TestLdapConfigRequest ldapValidationRequest) {
        return ldapConfigV1Service.testConnection(ldapValidationRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public CreateLdapConfigRequest getRequest(@ResourceCrn String environmentCrn) {
        return ldapConfigV1Service.getCreateLdapConfigRequest(environmentCrn);
    }
}

package com.sequenceiq.freeipa.ldap.v1;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
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
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    public DescribeLdapConfigResponse describe(String environmentId) {
        return ldapConfigV1Service.describe(environmentId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public DescribeLdapConfigResponse getForCluster(@TenantAwareParam String environmentCrn, String clusterName) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return ldapConfigV1Service.getForCluster(environmentCrn, accountId, clusterName);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_WRITE)
    public DescribeLdapConfigResponse create(CreateLdapConfigRequest request) {
        return ldapConfigV1Service.post(request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_WRITE)
    public void delete(String environmentId) {
        ldapConfigV1Service.delete(environmentId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    public TestLdapConfigResponse test(TestLdapConfigRequest ldapValidationRequest) {
        return ldapConfigV1Service.testConnection(ldapValidationRequest);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    public CreateLdapConfigRequest getRequest(String environmentId) {
        return ldapConfigV1Service.getCreateLdapConfigRequest(environmentId);
    }
}

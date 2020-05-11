package com.sequenceiq.freeipa.kerberos.v1;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.freeipa.api.v1.kerberos.KerberosConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.util.CrnService;
import com.sequenceiq.notification.NotificationController;

@Controller
@Transactional(TxType.NEVER)
@InternalReady
@AuthorizationResource
public class KerberosConfigV1Controller extends NotificationController implements KerberosConfigV1Endpoint {
    @Inject
    private KerberosConfigV1Service kerberosConfigV1Service;

    @Inject
    private CrnService crnService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    public DescribeKerberosConfigResponse describe(String environmentId) {
        return kerberosConfigV1Service.describe(environmentId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public DescribeKerberosConfigResponse getForCluster(@NotEmpty @TenantAwareParam String environmentCrn,
            @NotEmpty String clusterName) throws Exception {
        String accountId = crnService.getCurrentAccountId();
        return kerberosConfigV1Service.getForCluster(environmentCrn, accountId, clusterName);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_WRITE)
    public DescribeKerberosConfigResponse create(@Valid CreateKerberosConfigRequest request) {
        return kerberosConfigV1Service.post(request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_WRITE)
    public void delete(String environmentId) {
        kerberosConfigV1Service.delete(environmentId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    public CreateKerberosConfigRequest getRequest(String environmentId) {
        return kerberosConfigV1Service.getCreateRequest(environmentId);
    }
}

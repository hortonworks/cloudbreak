package com.sequenceiq.freeipa.kerberos.v1;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.freeipa.api.v1.kerberos.KerberosConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionWrapper;
import com.sequenceiq.freeipa.util.CrnService;
import com.sequenceiq.notification.WebSocketNotificationController;

@Controller
@Transactional(TxType.NEVER)
public class KerberosConfigV1Controller extends WebSocketNotificationController implements KerberosConfigV1Endpoint {
    @Inject
    private KerberosConfigV1Service kerberosConfigV1Service;

    @Inject
    private CrnService crnService;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public DescribeKerberosConfigResponse describe(@ResourceCrn String environmentCrn) {
        return kerberosConfigV1Service.describe(environmentCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public DescribeKerberosConfigResponse getForCluster(@ResourceCrn String environmentCrn, String clusterName) {
        String accountId = crnService.getCurrentAccountId();
        try {
            return kerberosConfigV1Service.getResponseForCluster(environmentCrn, accountId, clusterName);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public DescribeKerberosConfigResponse create(@RequestObject CreateKerberosConfigRequest request) {
        return kerberosConfigV1Service.post(request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public void delete(@ResourceCrn String environmentCrn) {
        kerberosConfigV1Service.delete(environmentCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public CreateKerberosConfigRequest getRequest(@ResourceCrn String environmentCrn) {
        return kerberosConfigV1Service.getCreateRequest(environmentCrn);
    }
}

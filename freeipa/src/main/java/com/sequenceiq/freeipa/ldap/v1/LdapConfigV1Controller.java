package com.sequenceiq.freeipa.ldap.v1;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.ldap.LdapConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.ldap.model.create.CreateLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigResponse;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionWrapper;
import com.sequenceiq.freeipa.service.binduser.UserSyncBindUserService;
import com.sequenceiq.freeipa.util.CrnService;
import com.sequenceiq.notification.WebSocketNotificationController;

@Controller
@Transactional(TxType.NEVER)
public class LdapConfigV1Controller extends WebSocketNotificationController implements LdapConfigV1Endpoint {
    @Inject
    private LdapConfigV1Service ldapConfigV1Service;

    @Inject
    private UserSyncBindUserService userSyncBindUserService;

    @Inject
    private CrnService crnService;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public DescribeLdapConfigResponse describe(@ResourceCrn String environmentCrn) {
        return ldapConfigV1Service.describe(environmentCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public DescribeLdapConfigResponse getForCluster(@ResourceCrn String environmentCrn, String clusterName) {
        String accountId = crnService.getCurrentAccountId();
        try {
            return ldapConfigV1Service.getForCluster(environmentCrn, accountId, clusterName);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public DescribeLdapConfigResponse create(@RequestObject CreateLdapConfigRequest request) {
        return ldapConfigV1Service.post(request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public void delete(@ResourceCrn String environmentCrn) {
        ldapConfigV1Service.delete(environmentCrn);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public TestLdapConfigResponse test(@RequestObject TestLdapConfigRequest ldapValidationRequest) {
        return ldapConfigV1Service.testConnection(ldapValidationRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public CreateLdapConfigRequest getRequest(@ResourceCrn String environmentCrn) {
        return ldapConfigV1Service.getCreateLdapConfigRequest(environmentCrn);
    }

    @Override
    @InternalOnly
    public DescribeLdapConfigResponse getForUserSync(@ResourceCrn String environmentCrn) {
        MDCBuilder.addEnvironmentCrn(environmentCrn);
        return userSyncBindUserService.getUserSyncLdapConfigIfExistsOrThrowNotFound(environmentCrn, Crn.safeFromString(environmentCrn).getAccountId());
    }
}

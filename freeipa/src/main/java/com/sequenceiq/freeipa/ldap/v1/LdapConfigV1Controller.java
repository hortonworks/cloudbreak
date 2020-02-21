package com.sequenceiq.freeipa.ldap.v1;

import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.freeipa.api.v1.ldap.LdapConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.ldap.model.create.CreateLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigResponse;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.util.CrnService;
import com.sequenceiq.notification.NotificationController;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

@Controller
@Transactional(TxType.NEVER)
@InternalReady
public class LdapConfigV1Controller extends NotificationController implements LdapConfigV1Endpoint {
    @Inject
    private LdapConfigV1Service ldapConfigV1Service;

    @Inject
    private CrnService crnService;

    @Override
    public DescribeLdapConfigResponse describe(String environmentId) {
        return ldapConfigV1Service.describe(environmentId);
    }

    @Override
    public DescribeLdapConfigResponse getForCluster(@ResourceCrn String environmentCrn, String clusterName) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return ldapConfigV1Service.getForCluster(environmentCrn, accountId, clusterName);
    }

    @Override
    public DescribeLdapConfigResponse create(CreateLdapConfigRequest request) {
        return ldapConfigV1Service.post(request);
    }

    @Override
    public void delete(String environmentId) {
        ldapConfigV1Service.delete(environmentId);
    }

    @Override
    public TestLdapConfigResponse test(TestLdapConfigRequest ldapValidationRequest) {
        return ldapConfigV1Service.testConnection(ldapValidationRequest);
    }

    @Override
    public CreateLdapConfigRequest getRequest(String environmentId) {
        return ldapConfigV1Service.getCreateLdapConfigRequest(environmentId);
    }
}

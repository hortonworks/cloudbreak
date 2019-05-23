package com.sequenceiq.freeipa.ldap.v1;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.freeipa.api.v1.ldap.LdapConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.ldap.model.create.CreateLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigResponse;
import com.sequenceiq.notification.NotificationController;

@Controller
@Transactional(TxType.NEVER)
public class LdapConfigV1Controller extends NotificationController implements LdapConfigV1Endpoint {
    @Inject
    private LdapConfigV1Service ldapConfigV1Service;

    @Override
    public DescribeLdapConfigResponse describe(String environmentId) {
        return ldapConfigV1Service.describe(environmentId);
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

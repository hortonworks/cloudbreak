package com.sequenceiq.freeipa.kerberos.v1;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.freeipa.api.v1.kerberos.KerberosConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;
import com.sequenceiq.notification.NotificationController;

@Controller
@Transactional(TxType.NEVER)
public class KerberosConfigV1Controller extends NotificationController implements KerberosConfigV1Endpoint {
    @Inject
    private KerberosConfigV1Service kerberosConfigV1Service;

    @Override
    public DescribeKerberosConfigResponse describe(String environmentId) {
        return kerberosConfigV1Service.describe(environmentId);
    }

    @Override
    public DescribeKerberosConfigResponse create(@Valid CreateKerberosConfigRequest request) {
        return kerberosConfigV1Service.post(request);
    }

    @Override
    public void delete(String environmentId) {
        kerberosConfigV1Service.delete(environmentId);
    }

    @Override
    public CreateKerberosConfigRequest getRequest(String environmentId) {
        return kerberosConfigV1Service.getCreateRequest(environmentId);
    }
}

package com.sequenceiq.freeipa.kerberosmgmt.v1;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.freeipa.api.v1.kerberosmgmt.KerberosMgmtV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class KerberosMgmtV1Controller implements KerberosMgmtV1Endpoint {
    @Inject
    private KerberosMgmtV1Service kerberosMgmtV1Service;

    @Inject
    private CrnService crnService;

    public ServiceKeytabResponse generateServiceKeytab(@Valid ServiceKeytabRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return kerberosMgmtV1Service.generateServiceKeytab(request, accountId);
    }

    public ServiceKeytabResponse getServiceKeytab(@Valid ServiceKeytabRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return kerberosMgmtV1Service.getExistingServiceKeytab(request, accountId);
    }

    public void deleteServicePrincipal(@Valid ServicePrincipalRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        kerberosMgmtV1Service.deleteServicePrincipal(request, accountId);
    }

    public void deleteHost(@Valid HostRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        kerberosMgmtV1Service.deleteHost(request, accountId);
    }
}

package com.sequenceiq.freeipa.kerberosmgmt.v1;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.freeipa.service.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class KerberosMgmtV1Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosMgmtV1Service.class);

    @Inject
    private CrnService crnService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    private boolean addService() {
        return true;
    }

    private boolean addHost() {
        return true;
    }

    public ServiceKeytabResponse generateServiceKeytab(ServiceKeytabRequest request) {
        ServiceKeytabResponse response = new ServiceKeytabResponse();
        // TODO add host and add pricipal before generating keytab.
        //TODO  Will be implemented CDPSDX-515
        response.setKeytab(null);
        return response;
    }

    public ServiceKeytabResponse getServiceKeytab(ServiceKeytabRequest request) {
        ServiceKeytabResponse response = new ServiceKeytabResponse();
        //TODO  Will be implemented CDPSDX-515
        response.setKeytab(null);
        return response;
    }

    public void deleteServicePrincipal(ServicePrincipalRequest request) {
        //TODO  Will be implemented CDPSDX-515
        return;
    }

    public void deleteHost(HostRequest request) {
        //TODO  Will be implemented CDPSDX-515
        return;
    }
}

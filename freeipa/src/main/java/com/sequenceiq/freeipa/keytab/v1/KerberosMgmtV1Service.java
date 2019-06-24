package com.sequenceiq.freeipa.keytab.v1;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.keytab.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.keytab.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
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
    StackService stackService;

    @Inject
    FreeIpaService freeIpaService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    private FreeIpaClient ipaClient = null;

    private boolean addService() {
        return true;
    }

    private boolean addHost() {
        return true;
    }

    public ServiceKeytabResponse getServiceKeytab(ServiceKeytabRequest request) {
        ServiceKeytabResponse response = new ServiceKeytabResponse();
        response.setId(request.getId());
        //TODO  Will be implemented CDPSDX-515
        response.setKeytab(null);
        response.setServicePrincial("kalyan/hostname1@CLOUDERA.COM");
        return response;
    }

    public void deletServiceKeytab(ServiceKeytabRequest request) {
        //TODO  Will be implemented CDPSDX-515
        return;
    }

}

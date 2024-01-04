package com.sequenceiq.datalake.service.sdx.flowcheck;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.flow.FreeIpaV1FlowEndpoint;

@Service
public class FreeipaFlowService extends AbstractFlowService {

    @Inject
    private FreeIpaV1FlowEndpoint freeIpaV1FlowEndpoint;

    @Override
    protected FlowEndpoint flowEndpoint() {
        return freeIpaV1FlowEndpoint;
    }
}

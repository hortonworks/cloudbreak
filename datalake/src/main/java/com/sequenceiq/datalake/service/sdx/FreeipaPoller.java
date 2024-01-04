package com.sequenceiq.datalake.service.sdx;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.flow.FreeIpaV1FlowEndpoint;

@Component
public class FreeipaPoller extends AbstractFlowPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaPoller.class);

    @Inject
    private FreeIpaV1FlowEndpoint freeIpaV1FlowEndpoint;

    @Override
    protected FlowEndpoint flowEndpoint() {
        return freeIpaV1FlowEndpoint;
    }
}

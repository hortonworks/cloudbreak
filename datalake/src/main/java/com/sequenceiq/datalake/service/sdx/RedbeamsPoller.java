package com.sequenceiq.datalake.service.sdx;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.redbeams.api.endpoint.v1.RedBeamsFlowEndpoint;

@Component
public class RedbeamsPoller extends AbstractFlowPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsPoller.class);

    @Inject
    private RedBeamsFlowEndpoint redbeamsFlowEndpoint;

    @Override
    protected FlowEndpoint flowEndpoint() {
        return redbeamsFlowEndpoint;
    }
}

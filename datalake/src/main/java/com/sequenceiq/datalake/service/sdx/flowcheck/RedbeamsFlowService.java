package com.sequenceiq.datalake.service.sdx.flowcheck;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.redbeams.api.endpoint.v1.RedBeamsFlowEndpoint;

@Service
public class RedbeamsFlowService extends AbstractFlowService {

    @Inject
    private RedBeamsFlowEndpoint redbeamsFlowEndpoint;

    @Override
    protected FlowEndpoint flowEndpoint() {
        return redbeamsFlowEndpoint;
    }
}

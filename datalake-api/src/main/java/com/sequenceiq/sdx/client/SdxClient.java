package com.sequenceiq.sdx.client;

import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;

public interface SdxClient {

    SdxInternalEndpoint sdxInternalEndpoint();

    SdxEndpoint sdxEndpoint();

    FlowEndpoint flowEndpoint();

}

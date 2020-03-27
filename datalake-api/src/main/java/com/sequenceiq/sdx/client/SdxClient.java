package com.sequenceiq.sdx.client;

import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;

public interface SdxClient {

    SdxInternalEndpoint sdxInternalEndpoint();

    SdxEndpoint sdxEndpoint();

    SdxUpgradeEndpoint sdxUpgradeEndpoint();

    FlowEndpoint flowEndpoint();

}

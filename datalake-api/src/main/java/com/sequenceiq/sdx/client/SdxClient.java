package com.sequenceiq.sdx.client;

import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;

public interface SdxClient {

    SdxInternalEndpoint sdxInternalEndpoint();

    SdxEndpoint sdxEndpoint();

}

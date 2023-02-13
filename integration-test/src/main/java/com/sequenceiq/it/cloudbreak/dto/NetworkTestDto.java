package com.sequenceiq.it.cloudbreak.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public class NetworkTestDto extends AbstractCloudbreakTestDto<NetworkV4Request, NetworkV4Response, NetworkTestDto> {

    protected NetworkTestDto(NetworkV4Request request, TestContext testContext) {
        super(request, testContext);
    }
}

package com.sequenceiq.it.cloudbreak.newway.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;

public class NetworkTestDto extends AbstractCloudbreakTestDto<NetworkV4Request, NetworkV4Response, NetworkTestDto> {
    public static final String NETWORK = "NETWORK";

    NetworkTestDto(String newId) {
        super(newId);
        setRequest(new NetworkV4Request());
    }

    NetworkTestDto() {
        this(NETWORK);
    }

}

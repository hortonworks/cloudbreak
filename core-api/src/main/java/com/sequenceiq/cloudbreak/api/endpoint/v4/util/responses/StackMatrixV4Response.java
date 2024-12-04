package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

public class StackMatrixV4Response {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, ClouderaManagerStackDescriptorV4Response> cdh = new HashMap<>();

    public Map<String, ClouderaManagerStackDescriptorV4Response> getCdh() {
        return cdh;
    }

    public void setCdh(Map<String, ClouderaManagerStackDescriptorV4Response> cdh) {
        this.cdh = cdh;
    }
}

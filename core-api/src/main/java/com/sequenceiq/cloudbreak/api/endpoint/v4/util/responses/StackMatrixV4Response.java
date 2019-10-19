package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Map;

public class StackMatrixV4Response {

    private Map<String, ClouderaManagerStackDescriptorV4Response> cdh;

    public Map<String, ClouderaManagerStackDescriptorV4Response> getCdh() {
        return cdh;
    }

    public void setCdh(Map<String, ClouderaManagerStackDescriptorV4Response> cdh) {
        this.cdh = cdh;
    }
}

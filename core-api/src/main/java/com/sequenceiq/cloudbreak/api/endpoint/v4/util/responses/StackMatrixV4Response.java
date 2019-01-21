package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Map;

public class StackMatrixV4Response {

    private Map<String, StackDescriptorV4Response> hdp;

    private Map<String, StackDescriptorV4Response> hdf;

    public Map<String, StackDescriptorV4Response> getHdp() {
        return hdp;
    }

    public void setHdp(Map<String, StackDescriptorV4Response> hdp) {
        this.hdp = hdp;
    }

    public Map<String, StackDescriptorV4Response> getHdf() {
        return hdf;
    }

    public void setHdf(Map<String, StackDescriptorV4Response> hdf) {
        this.hdf = hdf;
    }
}

package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Map;

public class StackMatrixV4Response {

    private Map<String, StackDescriptorV4> hdp;

    private Map<String, StackDescriptorV4> hdf;

    public Map<String, StackDescriptorV4> getHdp() {
        return hdp;
    }

    public void setHdp(Map<String, StackDescriptorV4> hdp) {
        this.hdp = hdp;
    }

    public Map<String, StackDescriptorV4> getHdf() {
        return hdf;
    }

    public void setHdf(Map<String, StackDescriptorV4> hdf) {
        this.hdf = hdf;
    }
}

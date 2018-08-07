package com.sequenceiq.cloudbreak.api.model.stack;

import java.util.Map;

public class StackMatrix {

    private Map<String, StackDescriptor> hdp;

    private Map<String, StackDescriptor> hdf;

    public Map<String, StackDescriptor> getHdp() {
        return hdp;
    }

    public void setHdp(Map<String, StackDescriptor> hdp) {
        this.hdp = hdp;
    }

    public Map<String, StackDescriptor> getHdf() {
        return hdf;
    }

    public void setHdf(Map<String, StackDescriptor> hdf) {
        this.hdf = hdf;
    }
}

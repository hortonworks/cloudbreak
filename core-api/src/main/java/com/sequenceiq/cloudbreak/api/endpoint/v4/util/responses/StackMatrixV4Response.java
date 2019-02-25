package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Map;

public class StackMatrixV4Response {

    private Map<String, AmbariStackDescriptorV4Response> hdp;

    private Map<String, AmbariStackDescriptorV4Response> hdf;

    private Map<String, ClouderaManagerStackDescriptorV4Response> cdh;

    public Map<String, AmbariStackDescriptorV4Response> getHdp() {
        return hdp;
    }

    public void setHdp(Map<String, AmbariStackDescriptorV4Response> hdp) {
        this.hdp = hdp;
    }

    public Map<String, AmbariStackDescriptorV4Response> getHdf() {
        return hdf;
    }

    public void setHdf(Map<String, AmbariStackDescriptorV4Response> hdf) {
        this.hdf = hdf;
    }

    public Map<String, ClouderaManagerStackDescriptorV4Response> getCdh() {
        return cdh;
    }

    public void setCdh(Map<String, ClouderaManagerStackDescriptorV4Response> cdh) {
        this.cdh = cdh;
    }
}

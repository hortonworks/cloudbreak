package com.sequenceiq.cloudbreak.ccm.cloudinit;

public class CcmConnectivityParameters {

    private CcmParameters ccmParameters;

    private CcmV2Parameters ccmV2Parameters;

    public CcmConnectivityParameters(CcmParameters ccmParameters) {
        this.ccmParameters = ccmParameters;
    }

    public CcmConnectivityParameters(CcmV2Parameters ccmV2Parameters) {
        this.ccmV2Parameters = ccmV2Parameters;
    }

    public CcmConnectivityParameters() {
    }

    public CcmParameters getCcmParameters() {
        return ccmParameters;
    }

    public void setCcmParameters(CcmParameters ccmParameters) {
        this.ccmParameters = ccmParameters;
    }

    public CcmV2Parameters getCcmV2Parameters() {
        return ccmV2Parameters;
    }

    public void setCcmV2Parameters(CcmV2Parameters ccmV2Parameters) {
        this.ccmV2Parameters = ccmV2Parameters;
    }

    public CcmConnectivityMode getConnectivityMode() {
        if (ccmParameters != null) {
            return CcmConnectivityMode.CCMV1;
        } else if (ccmV2Parameters != null) {
            return CcmConnectivityMode.CCMV2;
        } else {
            return CcmConnectivityMode.NONE;
        }
    }
}

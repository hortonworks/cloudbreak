package com.sequenceiq.cloudbreak.ccm.cloudinit;

public class CcmConnectivityParameters {

    private CcmParameters ccmParameters;

    private CcmV2Parameters ccmV2Parameters;

    private CcmConnectivityMode connectivityMode;

    public CcmConnectivityParameters(CcmParameters ccmParameters) {
        this.ccmParameters = ccmParameters;
        this.connectivityMode = CcmConnectivityMode.CCMV1;
    }

    public CcmConnectivityParameters(CcmV2Parameters ccmV2Parameters) {
        this.ccmV2Parameters = ccmV2Parameters;
        this.connectivityMode = CcmConnectivityMode.CCMV2;
    }

    public CcmConnectivityParameters(CcmConnectivityMode connectivityMode) {
        this.connectivityMode = connectivityMode;
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
        return connectivityMode;
    }

    public void setConnectivityMode(CcmConnectivityMode connectivityMode) {
        this.connectivityMode = connectivityMode;
    }
}

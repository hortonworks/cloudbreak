package com.sequenceiq.cloudbreak.ccm.cloudinit;

public class CcmConnectivityParameters {

    private CcmParameters ccmParameters;

    private CcmV2Parameters ccmV2Parameters;

    private CcmV2JumpgateParameters ccmV2JumpgateParameters;

    public CcmConnectivityParameters(CcmParameters ccmParameters) {
        this.ccmParameters = ccmParameters;
    }

    public CcmConnectivityParameters(CcmV2Parameters ccmV2Parameters) {
        this.ccmV2Parameters = ccmV2Parameters;
    }

    public CcmConnectivityParameters(CcmV2JumpgateParameters ccmV2JumpgateParameters) {
        this.ccmV2JumpgateParameters = ccmV2JumpgateParameters;
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

    public CcmV2JumpgateParameters getCcmV2JumpgateParameters() {
        return ccmV2JumpgateParameters;
    }

    public void setCcmV2JumpgateParameters(CcmV2JumpgateParameters ccmV2JumpgateParameters) {
        this.ccmV2JumpgateParameters = ccmV2JumpgateParameters;
    }

    public CcmConnectivityMode getConnectivityMode() {
        if (ccmParameters != null) {
            return CcmConnectivityMode.CCMV1;
        } else if (ccmV2Parameters != null) {
            return CcmConnectivityMode.CCMV2;
        } else if (ccmV2JumpgateParameters != null) {
            return CcmConnectivityMode.CCMV2_JUMPGATE;
        } else {
            return CcmConnectivityMode.NONE;
        }
    }
}

package com.sequenceiq.cloudbreak.ccm.cloudinit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Test;

public class CcmConnectivityParametersTest {

    private CcmConnectivityParameters underTest;

    @Test
    public void testConnectivityModeCcmV1() {
        CcmParameters ccmParameters = mock(CcmParameters.class);
        underTest = new CcmConnectivityParameters(ccmParameters);
        assertEquals(CcmConnectivityMode.CCMV1, underTest.getConnectivityMode(), "CcmConnectivityMode should be CCMV1");
    }

    @Test
    public void testConnectivityModeCcmV2() {
        CcmV2Parameters ccmV2Parameters = mock(CcmV2Parameters.class);
        underTest = new CcmConnectivityParameters(ccmV2Parameters);
        assertEquals(CcmConnectivityMode.CCMV2, underTest.getConnectivityMode(), "CcmConnectivityMode should be CCMV2");
    }

    @Test
    public void testConnectivityModeCcmV2Jumpgate() {
        CcmV2JumpgateParameters ccmV2JumpgateParameters = mock(CcmV2JumpgateParameters.class);
        underTest = new CcmConnectivityParameters(ccmV2JumpgateParameters);
        assertEquals(CcmConnectivityMode.CCMV2_JUMPGATE, underTest.getConnectivityMode(), "CcmConnectivityMode should be CCMV2_JUMPGATE");
    }

    @Test
    public void testConnectivityModeNone() {
        underTest = new CcmConnectivityParameters();
        assertEquals(CcmConnectivityMode.NONE, underTest.getConnectivityMode(), "CcmConnectivityMode should be NONE");
    }
}

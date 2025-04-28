package com.sequenceiq.cloudbreak.sdx.pdl.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PdlSdxStartStopServiceTest {

    private static final String PDL_CRN =  "crn:altus:environments:us-west-1:tenant:environment:crn1";

    @InjectMocks
    private PdlSdxStartStopService underTest;

    @Test
    public void testStartSdx() {
        underTest.startSdx(PDL_CRN);
    }

    @Test
    public void testStopSdx() {
        underTest.stopSdx(PDL_CRN);
    }
}

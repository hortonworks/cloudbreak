package com.sequenceiq.cloudbreak.sdx.pdl.service;

import static com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails.StatusEnum.AVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;

@ExtendWith(MockitoExtension.class)
public class PdlSdxDeleteServiceTest {

    private static final String PDL_CRN =  "crn:altus:environments:us-west-1:tenant:environment:crn1";

    @InjectMocks
    private PdlSdxDeleteService underTest;

    @Test
    public void testDelete() {
        underTest.deleteSdx(PDL_CRN, true);
    }

    @Test
    public void testGetDeletePollingResult() {
        assertEquals(PollingResult.COMPLETED, underTest.getDeletePollingResultByStatus(AVAILABLE));
    }
}

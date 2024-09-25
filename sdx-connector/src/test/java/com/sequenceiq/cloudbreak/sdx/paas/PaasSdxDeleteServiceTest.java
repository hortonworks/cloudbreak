package com.sequenceiq.cloudbreak.sdx.paas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxDeleteService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
public class PaasSdxDeleteServiceTest {

    private static final String PAAS_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";

    private static final String INTERNAL_ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    @Mock
    private SdxEndpoint sdxEndpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @InjectMocks
    private PaasSdxDeleteService underTest;

    @BeforeEach
    void setup() {
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        lenient().when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        lenient().when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_ACTOR);
    }

    @Test
    public void testDelete() {
        when(sdxEndpoint.deleteByCrn(any(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "1"));
        underTest.deleteSdx(PAAS_CRN, true);
        verify(sdxEndpoint).deleteByCrn(eq(PAAS_CRN), anyBoolean());
    }

    @Test
    public void testGetDeletePollingResult() {
        assertEquals(PollingResult.IN_PROGRESS, underTest.getDeletePollingResultByStatus(SdxClusterStatusResponse.STACK_DELETION_IN_PROGRESS));
        assertEquals(PollingResult.FAILED, underTest.getDeletePollingResultByStatus(SdxClusterStatusResponse.DELETE_FAILED));
    }
}

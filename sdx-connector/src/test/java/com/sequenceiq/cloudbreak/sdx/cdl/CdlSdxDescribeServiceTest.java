package com.sequenceiq.cloudbreak.sdx.cdl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcSdxCdlClient;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxStatusService;

@ExtendWith(MockitoExtension.class)
public class CdlSdxDescribeServiceTest {

    private static final String CDL_CRN = "crn:cdp:sdxsvc:us-west-1:tenant:instance:crn2";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:crn1";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private GrpcSdxCdlClient sdxClient;

    @InjectMocks
    private CdlSdxDescribeService underTest;

    @Test
    public void testListCrn() {
        setEnabled();
        CdlCrudProto.DatalakeResponse datalake = getDatalake();
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);
        when(sdxClient.findDatalake(anyString(), anyString())).thenReturn(datalake);
        assertTrue(underTest.listSdxCrns(ENV_CRN).contains(CDL_CRN));
        verify(sdxClient).findDatalake(eq(ENV_CRN), any());

        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.FALSE);
        assertTrue(underTest.listSdxCrns(ENV_CRN).isEmpty());
        verifyNoMoreInteractions(sdxClient);
    }

    private CdlCrudProto.DatalakeResponse getDatalake() {
        return CdlCrudProto.DatalakeResponse.newBuilder()
                .setCrn(CDL_CRN)
                .setName("dl-name")
                .setStatus("RUNNING")
                .build();
    }

    private void setEnabled() {
        Field cdlEnabled = ReflectionUtils.findField(CdlSdxStatusService.class, "cdlEnabled");
        ReflectionUtils.makeAccessible(cdlEnabled);
        ReflectionUtils.setField(cdlEnabled, underTest, true);
    }
}

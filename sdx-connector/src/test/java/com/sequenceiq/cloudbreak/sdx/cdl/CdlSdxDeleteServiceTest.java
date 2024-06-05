package com.sequenceiq.cloudbreak.sdx.cdl;

import static org.mockito.ArgumentMatchers.any;
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

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcSdxCdlClient;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxDeleteService;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxStatusService;

@ExtendWith(MockitoExtension.class)
public class CdlSdxDeleteServiceTest {

    private static final String CDL_CRN = "crn:cdp:sdxsvc:us-west-1:tenant:instance:crn2";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private GrpcSdxCdlClient sdxClient;

    @InjectMocks
    private CdlSdxDeleteService underTest;

    @Test
    public void testDelete() {
        setEnabled();
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);
        underTest.deleteSdx(CDL_CRN, true);
        verify(sdxClient).deleteDatalake(eq(CDL_CRN));

        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.FALSE);
        underTest.deleteSdx(CDL_CRN, true);
        verifyNoMoreInteractions(sdxClient);
    }

    private void setEnabled() {
        Field cdlEnabled = ReflectionUtils.findField(CdlSdxStatusService.class, "cdlEnabled");
        ReflectionUtils.makeAccessible(cdlEnabled);
        ReflectionUtils.setField(cdlEnabled, underTest, true);
    }
}

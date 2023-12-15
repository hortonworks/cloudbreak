package com.sequenceiq.cloudbreak.sdx.cdl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
public class CdlSdxServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:tenant:user:5678";

    private static final String CDL_CRN = "crn:cdp:sdxsvc:us-west-1:tenant:instance:crn2";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:crn1";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private GrpcSdxCdlClient sdxClient;

    @InjectMocks
    private CdlSdxService underTest;

    @Test
    public void testDelete() {
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);
        underTest.deleteSdx(CDL_CRN, true);
        verify(sdxClient).deleteDatalake(eq(CDL_CRN));

        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.FALSE);
        underTest.deleteSdx(CDL_CRN, true);
        verifyNoMoreInteractions(sdxClient);
    }

    @Test
    public void testListCrn() {
        CdlCrudProto.DatalakeResponse datalake = getDatalake();
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);
        when(sdxClient.findDatalake(anyString(), anyString())).thenReturn(datalake);
        assertTrue(underTest.listSdxCrns("envName", ENV_CRN).contains(CDL_CRN));
        verify(sdxClient).findDatalake(eq(ENV_CRN), any());

        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.FALSE);
        assertTrue(underTest.listSdxCrns("envName", ENV_CRN).isEmpty());
        verifyNoMoreInteractions(sdxClient);
    }

    @Test
    public void testListStatusPairsCrn() {
        CdlCrudProto.DatalakeResponse datalake = getDatalake();
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);
        when(sdxClient.findDatalake(anyString(), anyString())).thenReturn(datalake);
        assertTrue(ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.listSdxCrnStatusPair(ENV_CRN, "env", Set.of(CDL_CRN))
                .contains(Pair.of(CDL_CRN,  CdlCrudProto.StatusType.Value.RUNNING))));
        verify(sdxClient).findDatalake(anyString(), anyString());

        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.FALSE);
        assertTrue(ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.listSdxCrnStatusPair(ENV_CRN, "env", Set.of(CDL_CRN)).isEmpty()));
        verifyNoMoreInteractions(sdxClient);
    }

    private CdlCrudProto.DatalakeResponse getDatalake() {
        return CdlCrudProto.DatalakeResponse.newBuilder()
                .setCrn(CDL_CRN)
                .setName("dl-name")
                .setStatus("RUNNING")
                .build();
    }
}

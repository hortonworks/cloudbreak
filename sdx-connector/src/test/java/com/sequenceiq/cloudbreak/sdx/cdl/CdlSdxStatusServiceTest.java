package com.sequenceiq.cloudbreak.sdx.cdl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcSdxCdlClient;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxStatusService;

@ExtendWith(MockitoExtension.class)
public class CdlSdxStatusServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:tenant:user:5678";

    private static final String CDL_CRN = "crn:cdp:sdxsvc:us-west-1:tenant:instance:crn2";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:crn1";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private GrpcSdxCdlClient sdxClient;

    @InjectMocks
    private CdlSdxStatusService underTest;

    @Test
    public void testListStatusPairsCrn() {
        setEnabled();
        CdlCrudProto.DatalakeResponse datalake = getDatalake("RUNNING");
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);
        when(sdxClient.findDatalake(anyString(), anyString())).thenReturn(datalake);
        assertTrue(ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.listSdxCrnStatusPair(ENV_CRN, Set.of(CDL_CRN))
                .contains(Pair.of(CDL_CRN, CdlCrudProto.StatusType.Value.RUNNING))));
        verify(sdxClient).findDatalake(anyString(), anyString());

        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.FALSE);
        assertTrue(ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.listSdxCrnStatusPair(ENV_CRN, Set.of(CDL_CRN)).isEmpty()));
        verifyNoMoreInteractions(sdxClient);
    }

    @Test
    public void testListStatusPairsCrnWithDeletedAndNonDeletedCDL() {
        String otherCRN = "otherCRN";
        setEnabled();
        CdlCrudProto.DatalakeResponse datalakeNotDeleted = getDatalake("DELETING");
        CdlCrudProto.DatalakeResponse datalakeDeleted = getDatalake("DELETED");
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);
        when(sdxClient.findDatalake(anyString(), eq(CDL_CRN))).thenReturn(datalakeNotDeleted);
        when(sdxClient.findDatalake(any(), eq(otherCRN))).thenReturn(datalakeDeleted);
        assertTrue(ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.listSdxCrnStatusPair(ENV_CRN, Set.of(CDL_CRN, otherCRN))
                .contains(Pair.of(CDL_CRN, CdlCrudProto.StatusType.Value.DELETING))));
        verify(sdxClient, times(2)).findDatalake(anyString(), anyString());
    }

    private CdlCrudProto.DatalakeResponse getDatalake(String status) {
        return CdlCrudProto.DatalakeResponse.newBuilder()
                .setCrn(CDL_CRN)
                .setName("dl-name")
                .setStatus(status)
                .build();
    }

    private void setEnabled() {
        Field cdlEnabled = ReflectionUtils.findField(CdlSdxStatusService.class, "cdlEnabled");
        ReflectionUtils.makeAccessible(cdlEnabled);
        ReflectionUtils.setField(cdlEnabled, underTest, true);
    }
}

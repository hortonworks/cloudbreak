package com.sequenceiq.cloudbreak.saas.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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

import com.cloudera.thunderhead.service.sdxsvccommon.SDXSvcCommonProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.saas.client.sdx.GrpcSdxSaasClient;
import com.sequenceiq.cloudbreak.saas.sdx.polling.PollingResult;

@ExtendWith(MockitoExtension.class)
public class SaasSdxServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:tenant:user:5678";

    private static final String SAAS_CRN = "crn:cdp:sdxsvc:us-west-1:tenant:instance:crn2";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:crn1";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private GrpcSdxSaasClient grpcSdxSaasClient;

    @InjectMocks
    private SaasSdxService underTest;

    @Test
    public void testDelete() {
        doNothing().when(grpcSdxSaasClient).deleteInstance(any());
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);
        underTest.deleteSdx(SAAS_CRN, true);
        verify(grpcSdxSaasClient).deleteInstance(eq(SAAS_CRN));

        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.FALSE);
        underTest.deleteSdx(SAAS_CRN, true);
        verifyNoMoreInteractions(grpcSdxSaasClient);
    }

    @Test
    public void testListCrn() {
        SDXSvcCommonProto.Instance instance = getInstance();
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);
        when(grpcSdxSaasClient.listInstances(anyString())).thenReturn(Set.of(instance));
        assertTrue(underTest.listSdxCrns("envName", ENV_CRN).contains(SAAS_CRN));
        verify(grpcSdxSaasClient).listInstances(eq(ENV_CRN));

        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.FALSE);
        assertTrue(underTest.listSdxCrns("envName", ENV_CRN).isEmpty());
        verifyNoMoreInteractions(grpcSdxSaasClient);
    }

    @Test
    public void testListStatusPairsCrn() {
        SDXSvcCommonProto.Instance instance = getInstance();
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);
        when(grpcSdxSaasClient.listInstances(anyString())).thenReturn(Set.of(instance));
        assertTrue(ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.listSdxCrnStatusPair(ENV_CRN, "env", Set.of(SAAS_CRN))
                .contains(Pair.of(SAAS_CRN, SDXSvcCommonProto.InstanceHighLevelStatus.Value.HEALTHY))));
        verify(grpcSdxSaasClient).listInstances(anyString());

        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.FALSE);
        assertTrue(ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.listSdxCrnStatusPair(ENV_CRN, "env", Set.of(SAAS_CRN)).isEmpty()));
        verifyNoMoreInteractions(grpcSdxSaasClient);
    }

    @Test
    public void testGetDeletePollingResult() {
        assertEquals(PollingResult.IN_PROGRESS, underTest.getDeletePollingResultByStatus(SDXSvcCommonProto.InstanceHighLevelStatus.Value.HEALTHY));
        assertEquals(PollingResult.FAILED, underTest.getDeletePollingResultByStatus(SDXSvcCommonProto.InstanceHighLevelStatus.Value.UNHEALTHY));
    }

    private SDXSvcCommonProto.Instance getInstance() {
        return SDXSvcCommonProto.Instance.newBuilder()
                .setCrn(SAAS_CRN)
                .addEnvironments(ENV_CRN)
                .setStatus(SDXSvcCommonProto.InstanceHighLevelStatus.Value.HEALTHY)
                .build();
    }
}

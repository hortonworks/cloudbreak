package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RECOVERY_REQUESTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
public class SdxRecoveryServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com";

    private static final String CLUSTER_NAME = "dummyCluster";

    private static final AtomicLong CLUSTER_ID = new AtomicLong(20000L);

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    private SdxCluster cluster;

    @InjectMocks
    private SdxRecoveryService underTest;

    @BeforeEach
    public void setUp() {
        cluster = getValidSdxCluster();
    }

    @Test
    public void triggerCloudbreakRecovery() {

        Long clusterId = cluster.getId();
        doNothing().when(cloudbreakFlowService).saveLastCloudbreakFlowChainId(any(), any());
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "FLOW_ID_1");
        RecoveryV4Response recoveryV4Response = new RecoveryV4Response(flowId);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(sdxService.getById(clusterId)).thenReturn(cluster);
        when(ThreadBasedUserCrnProvider.doAsInternalActor(
                "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__",
                () ->
                stackV4Endpoint.recoverClusterByNameInternal(0L, cluster.getClusterName(),
                        ThreadBasedUserCrnProvider.getUserCrn()))).thenReturn(recoveryV4Response);

        ThreadBasedUserCrnProvider.doAsInternalActor("crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__", () -> underTest.recoverCluster(clusterId));

        verify(stackV4Endpoint).recoverClusterByNameInternal(eq(0L), eq(CLUSTER_NAME), nullable(String.class));
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.RECOVERY_IN_PROGRESS, DATALAKE_RECOVERY_REQUESTED,
                        "Recovering datalake stack", cluster);
    }

    private SdxCluster getValidSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setRuntime("7.2.0");
        sdxCluster.setId(CLUSTER_ID.incrementAndGet());
        sdxCluster.setAccountId("accountid");
        return sdxCluster;
    }

}
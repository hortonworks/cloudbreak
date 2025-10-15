package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class SdxUpgradePrepareServiceTest {

    private static final long STACK_ID = 1L;

    private static final String IMAGE_ID = "imageId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @InjectMocks
    private SdxUpgradePrepareService underTest;

    @Test
    public void testPrepareUpgrade() {
        SdxCluster cluster = new SdxCluster();
        cluster.setCrn("crn");
        when(sdxService.getById(STACK_ID)).thenReturn(cluster);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "pollId");
        when(stackV4Endpoint.prepareClusterUpgradeByCrnInternal(0L, cluster.getCrn(), IMAGE_ID, USER_CRN))
                .thenReturn(flowIdentifier);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.prepareUpgrade(STACK_ID, IMAGE_ID));

        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS, "Preparing Data Lake for upgrade",
                cluster);
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(cluster, flowIdentifier);
    }

    @Test
    public void testPrepareUpgradeWebAppEx() {
        SdxCluster cluster = new SdxCluster();
        cluster.setCrn("crn");
        when(sdxService.getById(STACK_ID)).thenReturn(cluster);
        WebApplicationException webApplicationException = new WebApplicationException();
        when(stackV4Endpoint.prepareClusterUpgradeByCrnInternal(0L, cluster.getCrn(), IMAGE_ID, USER_CRN))
                .thenThrow(webApplicationException);
        when(exceptionMessageExtractor.getErrorMessage(webApplicationException)).thenReturn("fail");

        assertThrows(CloudbreakServiceException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.prepareUpgrade(STACK_ID, IMAGE_ID)));

        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS, "Preparing Data Lake for upgrade",
                cluster);
        verifyNoInteractions(cloudbreakFlowService);
    }
}

package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.getSdxCluster;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@ExtendWith(MockitoExtension.class)
class ProxyConfigServiceTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    private static final String SDX_CRN = "crn";

    @Mock
    private StackService stackService;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @InjectMocks
    private ProxyConfigService underTest;

    @Test
    void modifyProxyConfig() {
        SdxCluster sdxCluster = getSdxCluster();
        String previousProxyConfigCrn = "previous-proxy-crn";
        FlowIdentifier cbFlowIdentifier = mock(FlowIdentifier.class);
        when(stackService.modifyProxyConfigInternal(SDX_CRN, previousProxyConfigCrn)).thenReturn(cbFlowIdentifier);
        FlowIdentifier sdxFlowIdentifier = mock(FlowIdentifier.class);
        when(sdxReactorFlowManager.triggerModifyProxyConfigTracker(sdxCluster)).thenReturn(sdxFlowIdentifier);

        FlowIdentifier result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.modifyProxyConfig(sdxCluster, previousProxyConfigCrn));

        assertEquals(sdxFlowIdentifier, result);
        verify(stackService).modifyProxyConfigInternal(SDX_CRN, previousProxyConfigCrn);
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(sdxCluster, cbFlowIdentifier);
        verify(sdxReactorFlowManager).triggerModifyProxyConfigTracker(sdxCluster);
    }

}
package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.function.Supplier;

import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.RdsUpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
public class CloudbreakStackServiceTest {

    private static final String SDX_NAME = "sdxName";

    private static final String SDX_ACCOUNT_ID = "sdxAccountId";

    private static final long WORKSPACE_ID = 0L;

    private static final String USER_CRN = "userCrn";

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @InjectMocks
    private CloudbreakStackService underTest;

    @Test
    void testGetStack() {
        SdxCluster sdxCluster = setupSdxCluster();
        setupIam();
        StackV4Response stackV4Response = new StackV4Response();
        when(stackV4Endpoint.get(WORKSPACE_ID, SDX_NAME, Set.of(), SDX_ACCOUNT_ID)).thenReturn(stackV4Response);

        StackV4Response response = underTest.getStack(sdxCluster);

        assertEquals(stackV4Response, response);
        verify(stackV4Endpoint).get(WORKSPACE_ID, SDX_NAME, Set.of(), SDX_ACCOUNT_ID);
    }

    @Test
    void testGetStackWhenWebApplicationException() {
        SdxCluster sdxCluster = setupSdxCluster();
        setupIam();
        when(stackV4Endpoint.get(WORKSPACE_ID, SDX_NAME, Set.of(), SDX_ACCOUNT_ID)).thenThrow(WebApplicationException.class);

        Assertions.assertThrows(CloudbreakApiException.class, () ->
                underTest.getStack(sdxCluster)
        );
    }

    @Test
    void testUpgradeRdsByClusterNameInternal() {
        try (MockedStatic<ThreadBasedUserCrnProvider> threadBasedUserCrnProvider = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            SdxCluster sdxCluster = setupSdxCluster();
            setupIam();
            TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
            FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "pollableId");
            RdsUpgradeV4Response rdsUpgradeV4Response = new RdsUpgradeV4Response();
            rdsUpgradeV4Response.setFlowIdentifier(flowIdentifier);
            threadBasedUserCrnProvider.when(ThreadBasedUserCrnProvider::getUserCrn).thenReturn(USER_CRN);
            threadBasedUserCrnProvider.when(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(), any(Supplier.class))).thenReturn(rdsUpgradeV4Response);

            RdsUpgradeV4Response response = underTest.upgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion);

            assertEquals(rdsUpgradeV4Response, response);
            verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        }
    }

    private static SdxCluster setupSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(SDX_NAME);
        sdxCluster.setAccountId(SDX_ACCOUNT_ID);
        return sdxCluster;
    }

    private void setupIam() {
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("internalCrn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
    }
}

package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.getImage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionChecker;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerFactory;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerType;
import com.sequenceiq.cloudbreak.conclusion.ConclusionResult;
import com.sequenceiq.cloudbreak.conclusion.step.Conclusion;
import com.sequenceiq.cloudbreak.conclusion.step.ConclusionStep;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailHandledRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedRequest;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncerService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeFailedHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    @Mock
    private CmSyncerService cmSyncerService;

    @Mock
    private StackService stackService;

    @Mock
    private ConclusionCheckerFactory conclusionCheckerFactory;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ClusterUpgradeService clusterUpgradeService;

    @InjectMocks
    private ClusterUpgradeFailedHandler underTest;

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testHandleClusterUpgradeFailedRequestWhenNoConclusionFound(boolean conclusionFound) {
        ReflectionTestUtils.setField(underTest, "syncAfterFailureEnabled", true);
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(TestUtil.stack());
        ConclusionChecker conclusionChecker = mock(ConclusionChecker.class);
        ConclusionResult conclusionResult = conclusionFound
                ? new ConclusionResult(List.of(Conclusion.failed("error", "details", ConclusionStep.class)))
                : new ConclusionResult(List.of());
        when(conclusionChecker.doCheck(eq(STACK_ID))).thenReturn(conclusionResult);
        when(conclusionCheckerFactory.getConclusionChecker(eq(ConclusionCheckerType.DEFAULT))).thenReturn(conclusionChecker);
        when(entitlementService.conclusionCheckerSendUserEventEnabled(anyString())).thenReturn(Boolean.TRUE);
        Set<Image> images = Set.of(getImage(true, "uuid1", "7.1.0"), getImage(true, "uuid2", "7.2.1"));
        ClusterUpgradeFailedRequest request = new ClusterUpgradeFailedRequest(STACK_ID, new RuntimeException("error"),
                DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED, images);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.doAccept(handlerEvent));

        assertThat(selectable).isInstanceOf(ClusterUpgradeFailHandledRequest.class);

        verify(stackService, times(1)).getByIdWithListsInTransaction(eq(STACK_ID));
        verify(conclusionChecker, times(1)).doCheck(eq(STACK_ID));
        VerificationMode verificationMode = conclusionFound ? times(1) : never();
        verify(flowMessageService, verificationMode).fireEventAndLog(anyLong(), anyString(), any(), anyString());
        verify(clusterUpgradeService, times(1)).handleUpgradeClusterFailure(eq(STACK_ID), eq("error"),
                eq(DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED));
    }

}
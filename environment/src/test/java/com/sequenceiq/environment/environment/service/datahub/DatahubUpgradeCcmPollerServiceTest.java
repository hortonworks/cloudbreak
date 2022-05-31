package com.sequenceiq.environment.environment.service.datahub;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class DatahubUpgradeCcmPollerServiceTest {

    private static final long ENV_ID = 123L;

    @Mock
    private DatahubPollerProvider pollerProvider;

    @Mock
    private MultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    @InjectMocks
    private DatahubUpgradeCcmPollerService underTest;

    private List<FlowIdentifier> flowIds;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "attempt", 1);
        ReflectionTestUtils.setField(underTest, "sleeptime", 1);
        FlowIdentifier flowid1 = createFlowIdentifier();
        FlowIdentifier flowid2 = createFlowIdentifier();
        flowIds = List.of(flowid1, flowid2);
    }

    @Test
    void waitForUpgradeCcm() {
        AttemptMaker<Void> attemptMaker = AttemptResults::justFinish;
        when(pollerProvider.upgradeCcmPoller(ENV_ID, flowIds)).thenReturn(attemptMaker);
        when(multipleFlowsResultEvaluator.anyFailed(flowIds)).thenReturn(false);
        underTest.waitForUpgradeOnFlowIds(ENV_ID, flowIds);
        verify(pollerProvider).upgradeCcmPoller(ENV_ID, flowIds);
    }

    @Test
    void waitForUpgradeCcmNoFlows() {
        AttemptMaker<Void> attemptMaker = AttemptResults::justFinish;
        when(pollerProvider.upgradeCcmPoller(ENV_ID, List.of())).thenReturn(attemptMaker);
        when(multipleFlowsResultEvaluator.anyFailed(List.of())).thenReturn(false);
        underTest.waitForUpgradeOnFlowIds(ENV_ID, List.of());
        verify(pollerProvider).upgradeCcmPoller(ENV_ID, List.of());
    }

    @Test
    void pollerException() {
        when(pollerProvider.upgradeCcmPoller(ENV_ID, flowIds))
                .thenReturn(() -> {
                    throw new IllegalStateException("error");
                });
        assertThatThrownBy(() -> underTest.waitForUpgradeOnFlowIds(ENV_ID, flowIds))
                .isInstanceOf(DatahubOperationFailedException.class);
        verify(pollerProvider).upgradeCcmPoller(ENV_ID, flowIds);
    }

    @Test
    void waitForUpgradeCcmAnyFlowFailed() {
        AttemptMaker<Void> attemptMaker = AttemptResults::justFinish;
        when(pollerProvider.upgradeCcmPoller(ENV_ID, flowIds)).thenReturn(attemptMaker);
        when(multipleFlowsResultEvaluator.anyFailed(flowIds)).thenReturn(true);
        assertThatThrownBy(() -> underTest.waitForUpgradeOnFlowIds(ENV_ID, flowIds))
                .isInstanceOf(DatahubOperationFailedException.class);
        verify(pollerProvider).upgradeCcmPoller(ENV_ID, flowIds);
    }

    private FlowIdentifier createFlowIdentifier() {
        return new FlowIdentifier(FlowType.FLOW, UUID.randomUUID().toString());
    }

}

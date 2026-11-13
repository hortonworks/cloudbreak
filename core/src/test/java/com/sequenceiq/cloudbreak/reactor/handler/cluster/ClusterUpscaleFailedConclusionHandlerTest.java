package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerService;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerType;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterUpscaleFailedConclusionRequest;
import com.sequenceiq.cloudbreak.service.diagnostics.CmDiagnosticsCommandHintService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpscaleFailedConclusionHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private ConclusionCheckerService conclusionCheckerService;

    @Mock
    private CmDiagnosticsCommandHintService cmDiagnosticsCommandHintService;

    @InjectMocks
    private ClusterUpscaleFailedConclusionHandler underTest;

    @Test
    void firesCmDiagnosticsHintWhenRepair() {
        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request(true))));

        assertThat(result).isInstanceOf(StackEvent.class);
        verify(conclusionCheckerService).runConclusionChecker(STACK_ID, UPDATE_FAILED.name(), CLUSTER_SCALING_FAILED,
                ConclusionCheckerType.DEFAULT, "added to");
        verify(cmDiagnosticsCommandHintService).fireCmDiagnosticsHint(STACK_ID, List.of());
    }

    @Test
    void doesNotFireCmDiagnosticsHintWhenNotRepair() {
        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request(false))));

        assertThat(result).isInstanceOf(StackEvent.class);
        verify(cmDiagnosticsCommandHintService, never()).fireCmDiagnosticsHint(STACK_ID, List.of());
    }

    private ClusterUpscaleFailedConclusionRequest request(boolean repair) {
        return new ClusterUpscaleFailedConclusionRequest(STACK_ID, repair);
    }
}

package com.sequenceiq.cloudbreak.reactor;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionResult;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.repair.CandidateUnhealthyInstanceSelector;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstancesFinalizer;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
class UnhealthyInstancesDetectionHandlerTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CandidateUnhealthyInstanceSelector candidateUnhealthyInstanceSelector;

    @Mock
    private UnhealthyInstancesFinalizer unhealthyInstancesFinalizer;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private UnhealthyInstancesDetectionHandler unhealthyInstancesDetectionHandler;

    @Test
    void shouldNotInvokeFinalizerIfNoCandidateUnhealthyInstancesWereSelected() {

        long stackId = 1L;
        UnhealthyInstancesDetectionRequest unhealthyInstancesDetectionRequest = new UnhealthyInstancesDetectionRequest(stackId);
        Event<UnhealthyInstancesDetectionRequest> event = mock(Event.class);
        when(event.getData()).thenReturn(unhealthyInstancesDetectionRequest);

        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(stackId);
        when(stackDtoService.getStackViewById(stackId)).thenReturn(stack);

        when(candidateUnhealthyInstanceSelector.selectCandidateUnhealthyInstances(stackId)).thenReturn(Collections.emptySet());

        unhealthyInstancesDetectionHandler.accept(event);
        verifyNoInteractions(unhealthyInstancesFinalizer);

        verify(eventBus).notify(eq(EventSelectorUtil.selector(UnhealthyInstancesDetectionResult.class)),
                argThat(new UnhealthyInstancesResultMatcher(Collections.emptySet())));
    }

    @Test
    void shouldCreateResponseWithExactInstances() {
        long stackId = 1L;
        UnhealthyInstancesDetectionRequest unhealthyInstancesDetectionRequest = new UnhealthyInstancesDetectionRequest(stackId);
        Event<UnhealthyInstancesDetectionRequest> event = mock(Event.class);
        when(event.getData()).thenReturn(unhealthyInstancesDetectionRequest);

        StackView stack = mock(StackView.class);
        when(stackDtoService.getStackViewById(stackId)).thenReturn(stack);
        Set<InstanceMetadataView> unhealthyInstances = new HashSet<>();
        InstanceMetaData imd1 = mock(InstanceMetaData.class);
        InstanceMetaData imd2 = mock(InstanceMetaData.class);
        InstanceMetaData imd3 = mock(InstanceMetaData.class);
        unhealthyInstances.add(imd1);
        unhealthyInstances.add(imd2);
        unhealthyInstances.add(imd3);
        when(candidateUnhealthyInstanceSelector.selectCandidateUnhealthyInstances(stack.getId())).thenReturn(unhealthyInstances);

        Set<String> unhealthyInstanceIds = new HashSet<>();
        unhealthyInstanceIds.add("i-0f1e0605506aaaaaa");
        unhealthyInstanceIds.add("i-0f1e0605506cccccc");
        when(unhealthyInstancesFinalizer.finalizeUnhealthyInstances(stack, unhealthyInstances)).thenReturn(unhealthyInstanceIds);

        unhealthyInstancesDetectionHandler.accept(event);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(UnhealthyInstancesDetectionResult.class)),
                argThat(new UnhealthyInstancesResultMatcher(unhealthyInstanceIds)));

    }

    private static class UnhealthyInstancesResultMatcher implements ArgumentMatcher<Event<UnhealthyInstancesDetectionResult>> {

        private final Set<String> expectedUnhealthyIds;

        private String errorMessage;

        private UnhealthyInstancesResultMatcher(Set<String> expectedUnhealthyIds) {
            this.expectedUnhealthyIds = expectedUnhealthyIds;
        }

        @Override
        public String toString() {
            return errorMessage == null ? "" : errorMessage;
        }

        @Override
        public boolean matches(Event<UnhealthyInstancesDetectionResult> event) {
            UnhealthyInstancesDetectionResult payload = event.getData();
            Set<String> unhealthyInstanceIds = payload.getUnhealthyInstanceIds();
            if (unhealthyInstanceIds.size() != expectedUnhealthyIds.size()) {
                errorMessage = String.format("Sizes don't match, expected=%d, actual=%d",
                        expectedUnhealthyIds.size(), unhealthyInstanceIds.size());
                return false;
            }
            for (String i : expectedUnhealthyIds) {
                if (!unhealthyInstanceIds.contains(i)) {
                    errorMessage = String.format("Expected unhealthy instance id %s not found in actual instances %s",
                            i, unhealthyInstanceIds);
                    return false;
                }
            }
            return true;
        }
    }
}

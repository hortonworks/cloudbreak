package com.sequenceiq.cloudbreak.reactor;

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.repair.CandidateUnhealthyInstanceSelector;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstancesFinalizer;
import org.hamcrest.Description;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import reactor.bus.Event;
import reactor.bus.EventBus;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.argThat;


@RunWith(MockitoJUnitRunner.class)
public class UnhealthyInstancesDetectionHandlerTest {

    @Mock
    private StackService stackService;

    @Mock
    private CandidateUnhealthyInstanceSelector candidateUnhealthyInstanceSelector;

    @Mock
    private UnhealthyInstancesFinalizer unhealthyInstancesFinalizer;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private UnhealthyInstancesDetectionHandler unhealthyInstancesDetectionHandler;

    @Test
    public void shouldNotInvokeFinalizerIfNoCandidateUnhealthyInstancesWereSelected() throws CloudbreakSecuritySetupException {

        long stackId = 1L;
        UnhealthyInstancesDetectionRequest unhealthyInstancesDetectionRequest = new UnhealthyInstancesDetectionRequest(stackId);
        Event event = mock(Event.class);
        when(event.getData()).thenReturn(unhealthyInstancesDetectionRequest);

        Stack stack = mock(Stack.class);
        when(stackService.getById(stackId)).thenReturn(stack);

        when(candidateUnhealthyInstanceSelector.selectCandidateUnhealthyInstances(stack)).thenReturn(Collections.EMPTY_SET);

        unhealthyInstancesDetectionHandler.accept(event);
        verifyZeroInteractions(unhealthyInstancesFinalizer);

        verify(eventBus).notify(eq(EventSelectorUtil.selector(UnhealthyInstancesDetectionResult.class)),
                argThat(new UnhealthyInstancesResultMatcher(Collections.EMPTY_SET)));
    }

    @Test
    public void shouldCreateResponseWithExactInstances() throws CloudbreakSecuritySetupException {
        long stackId = 1L;
        UnhealthyInstancesDetectionRequest unhealthyInstancesDetectionRequest = new UnhealthyInstancesDetectionRequest(stackId);
        Event event = mock(Event.class);
        when(event.getData()).thenReturn(unhealthyInstancesDetectionRequest);

        Stack stack = mock(Stack.class);
        when(stackService.getById(stackId)).thenReturn(stack);
        Set<InstanceMetaData> unhealthyInstances = new HashSet<>();
        InstanceMetaData imd1 = mock(InstanceMetaData.class);
        InstanceMetaData imd2 = mock(InstanceMetaData.class);
        InstanceMetaData imd3 = mock(InstanceMetaData.class);
        unhealthyInstances.add(imd1);
        unhealthyInstances.add(imd2);
        unhealthyInstances.add(imd3);
        when(candidateUnhealthyInstanceSelector.selectCandidateUnhealthyInstances(stack)).thenReturn(unhealthyInstances);

        Set<String> unhealthyInstanceIds = new HashSet<>();
        unhealthyInstanceIds.add("i-0f1e0605506aaaaaa");
        unhealthyInstanceIds.add("i-0f1e0605506cccccc");
        when(unhealthyInstancesFinalizer.finalizeUnhealthyInstances(stack, unhealthyInstances)).thenReturn(unhealthyInstanceIds);

        unhealthyInstancesDetectionHandler.accept(event);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(UnhealthyInstancesDetectionResult.class)),
                argThat(new UnhealthyInstancesResultMatcher(unhealthyInstanceIds)));

    }

    private class UnhealthyInstancesResultMatcher extends ArgumentMatcher<Event<UnhealthyInstancesDetectionResult>> {

        private Set<String> expectedUnhealthyIds;

        private String errorMessage;

        UnhealthyInstancesResultMatcher(Set<String> expectedUnhealthyIds) {
            this.expectedUnhealthyIds = expectedUnhealthyIds;
        }

        @Override
        public boolean matches(Object argument) {
            Event event = (Event) argument;
            UnhealthyInstancesDetectionResult payload = (UnhealthyInstancesDetectionResult) event.getData();
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

        @Override
        public void describeTo(Description description) {
            if (errorMessage == null) {
                super.describeTo(description);
            } else {
                description.appendText(errorMessage);
            }
        }
    }
}

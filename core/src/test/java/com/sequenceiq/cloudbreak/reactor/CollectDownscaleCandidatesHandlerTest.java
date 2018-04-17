package com.sequenceiq.cloudbreak.reactor;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariDecommissioner;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@RunWith(MockitoJUnitRunner.class)
public class CollectDownscaleCandidatesHandlerTest {

    private static final long STACK_ID = 1;

    private static final String HOST_GROUP_NAME = "HostGroupName";

    @Mock
    private EventBus eventBus;

    @Mock
    private StackService stackService;

    @Mock
    private AmbariDecommissioner ambariDecommissioner;

    @InjectMocks
    private CollectDownscaleCandidatesHandler collectDownscaleCandidatesHandler;

    private final Stack stack = new Stack();

    @Before
    public void init() {
        stack.setCluster(new Cluster());
    }

    @Test
    public void shouldCreateErrorResultWhenFqdnAndSaIsMissing() {
        CollectDownscaleCandidatesRequest request
                = new CollectDownscaleCandidatesRequest(STACK_ID, HOST_GROUP_NAME, null, Collections.singleton(null));
        Event<CollectDownscaleCandidatesRequest> event = new Event<>(request);

        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        collectDownscaleCandidatesHandler.accept(event);
        verify(eventBus).notify(eq(EventSelectorUtil.failureSelector(CollectDownscaleCandidatesResult.class)),
                argThat(new CollectDownscaleCandidatesResultMatcher(Collections.emptySet(), CollectDownscaleCandidatesHandler.ERROR_STATUS_REASON)));
    }

    @Test
    public void shouldCollectDownscaleCandidatesWhenFqdnIsMissing() throws CloudbreakException {
        int scalingAdjustment = 2;
        CollectDownscaleCandidatesRequest request
                = new CollectDownscaleCandidatesRequest(STACK_ID, HOST_GROUP_NAME, scalingAdjustment, Collections.singleton(""));
        Event<CollectDownscaleCandidatesRequest> event = new Event<>(request);

        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        collectDownscaleCandidatesHandler.accept(event);
        verify(ambariDecommissioner).collectDownscaleCandidates(stack, HOST_GROUP_NAME, scalingAdjustment);
    }

    @Test
    public void shouldVerifyNodeCountWhenNothingIsMissing() throws CloudbreakException {
        int scalingAdjustment = 2;
        String fqdn = "fqdn";
        CollectDownscaleCandidatesRequest request
                = new CollectDownscaleCandidatesRequest(STACK_ID, HOST_GROUP_NAME, scalingAdjustment, Collections.singleton(fqdn));
        Event<CollectDownscaleCandidatesRequest> event = new Event<>(request);

        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        collectDownscaleCandidatesHandler.accept(event);
        verify(ambariDecommissioner).verifyNodeCount(stack, stack.getCluster(), fqdn);
    }

    private static class CollectDownscaleCandidatesResultMatcher extends ArgumentMatcher<Event<CollectDownscaleCandidatesResult>> {

        private final Set<String> expectedHostNames;

        private final String expectedStatusReason;

        private final StringBuilder statusDescription = new StringBuilder();

        CollectDownscaleCandidatesResultMatcher(Set<String> expectedHostNames, String expectedStatusReason) {
            this.expectedHostNames = expectedHostNames;
            this.expectedStatusReason = expectedStatusReason;
        }

        @Override
        public boolean matches(Object argument) {
            Event event = (Event) argument;
            CollectDownscaleCandidatesResult payload = (CollectDownscaleCandidatesResult) event.getData();
            boolean matches = areHostNamesMatching(payload);
            String statusReason = payload.getStatusReason();
            if (!StringUtils.equals(expectedStatusReason, statusReason)) {
                statusDescription
                        .append(String.format("Expected and actual statusReason does not match. %n expected=%s, %n actual=%s",
                                expectedStatusReason, statusReason));
                matches = false;
            }
            return matches;
        }

        private boolean areHostNamesMatching(CollectDownscaleCandidatesResult payload) {
            Set<String> hostNames = payload.getHostNames();
            if (hostNames == null && expectedHostNames == null) {
                return true;
            }
            if (hostNames == null || expectedHostNames == null) {
                statusDescription
                        .append(String.format("Expected or actual hostNames is null. %n expected=%s, %n actual=%s. %n",
                                expectedHostNames, hostNames));
                return false;
            }
            boolean matches = true;
            if (hostNames.size() != expectedHostNames.size()) {
                statusDescription
                        .append(String.format("Sizes don't match, %n expected=%d, %n actual=%d. %n",
                                expectedHostNames.size(), hostNames.size()));
                matches = false;
            }
            if (hostNames.isEmpty() && expectedHostNames.isEmpty()) {
                return matches;
            }
            if (hostNames.containsAll(expectedHostNames) && expectedHostNames.containsAll(hostNames)) {
                statusDescription
                        .append("Expected and actual hostNames sets contents differ.\n")
                        .append("Expected: ").append(Arrays.toString(expectedHostNames.toArray())).append('\n')
                        .append("Actual: ").append(Arrays.toString(hostNames.toArray())).append('\n');
            }
            return matches;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(statusDescription.toString());
        }
    }
}
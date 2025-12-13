package com.sequenceiq.environment.environment.service.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.environment.environment.poller.StackPollerProvider;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;

@ExtendWith(MockitoExtension.class)
class StackPollerServiceTest {

    private static final Long ENVIRONMENT_ID = 123L;

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String FLOW_ID = "flowId";

    private static final String STACK_CRN_1 = "stackCrn1";

    private static final String STACK_CRN_2 = "stackCrn2";

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private StackPollerProvider stackPollerProvider;

    @InjectMocks
    private StackPollerService underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "maxTime", 3);
        ReflectionTestUtils.setField(underTest, "sleepTime", 1);
    }

    @Test
    void updateStackConfigurationsWhenNoExistingStacks() {
        when(stackV4Endpoint.list(0L, ENVIRONMENT_CRN, false)).thenReturn(new StackViewV4Responses());
        when(stackPollerProvider.stackUpdateConfigPoller(List.of(), ENVIRONMENT_ID, FLOW_ID)).thenReturn(() -> AttemptResults.finishWith(null));

        underTest.updateStackConfigurations(ENVIRONMENT_ID, ENVIRONMENT_CRN, FLOW_ID);
    }

    @Test
    void updateStackConfigurationsWhenNoStacksToUpdate() {
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(
                Set.of(createStackViewV4ResponseWithStatus(Status.STOPPED), createStackViewV4ResponseWithStatus(Status.DELETE_FAILED)));
        when(stackV4Endpoint.list(0L, ENVIRONMENT_CRN, false)).thenReturn(stackViewV4Responses);

        when(stackPollerProvider.stackUpdateConfigPoller(List.of(), ENVIRONMENT_ID, FLOW_ID)).thenReturn(() -> AttemptResults.finishWith(null));

        underTest.updateStackConfigurations(ENVIRONMENT_ID, ENVIRONMENT_CRN, FLOW_ID);
    }

    private StackViewV4Response createStackViewV4ResponseWithStatus(Status status) {
        StackViewV4Response stack = new StackViewV4Response();
        ClusterViewV4Response cluster = new ClusterViewV4Response();
        cluster.setStatus(status);
        stack.setCluster(cluster);
        return stack;
    }

    @Test
    void updateStackConfigurationsWhenStacksNeedToBeUpdated() {
        Set<StackViewV4Response> responsesSet = new LinkedHashSet<>();
        responsesSet.add(createAvailableStackViewV4Response(STACK_CRN_1));
        responsesSet.add(createAvailableStackViewV4Response(STACK_CRN_2));
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(responsesSet);
        when(stackV4Endpoint.list(0L, ENVIRONMENT_CRN, false)).thenReturn(stackViewV4Responses);

        when(stackPollerProvider.stackUpdateConfigPoller(List.of(STACK_CRN_1, STACK_CRN_2), ENVIRONMENT_ID, FLOW_ID))
                .thenReturn(() -> AttemptResults.finishWith(null));

        underTest.updateStackConfigurations(ENVIRONMENT_ID, ENVIRONMENT_CRN, FLOW_ID);
    }

    private StackViewV4Response createAvailableStackViewV4Response(String stackCrn) {
        StackViewV4Response stack = createStackViewV4ResponseWithStatus(Status.AVAILABLE);
        stack.setCrn(stackCrn);
        return stack;
    }

    @Test
    void updateStackConfigurationsWhenPollerTimeout() {
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(createAvailableStackViewV4Response(STACK_CRN_1)));
        when(stackV4Endpoint.list(0L, ENVIRONMENT_CRN, false)).thenReturn(stackViewV4Responses);

        when(stackPollerProvider.stackUpdateConfigPoller(List.of(STACK_CRN_1), ENVIRONMENT_ID, FLOW_ID)).thenReturn(AttemptResults::justContinue);

        DatahubOperationFailedException datahubOperationFailedException = assertThrows(DatahubOperationFailedException.class,
                () -> underTest.updateStackConfigurations(ENVIRONMENT_ID, ENVIRONMENT_CRN, FLOW_ID));

        assertThat(datahubOperationFailedException).hasMessage("Stack config updating timed out");
        assertThat(datahubOperationFailedException).hasCauseInstanceOf(PollerStoppedException.class);
    }

    @Test
    void updateStackConfigurationsWhenPollerAbortWithError() {
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(createAvailableStackViewV4Response(STACK_CRN_1)));
        when(stackV4Endpoint.list(0L, ENVIRONMENT_CRN, false)).thenReturn(stackViewV4Responses);

        when(stackPollerProvider.stackUpdateConfigPoller(List.of(STACK_CRN_1), ENVIRONMENT_ID, FLOW_ID))
                .thenReturn(() -> AttemptResults.breakFor(new Exception("Foo")));

        DatahubOperationFailedException datahubOperationFailedException = assertThrows(DatahubOperationFailedException.class,
                () -> underTest.updateStackConfigurations(ENVIRONMENT_ID, ENVIRONMENT_CRN, FLOW_ID));

        assertThat(datahubOperationFailedException).hasMessage("Stack config updating aborted with error");
        assertThat(datahubOperationFailedException).hasCauseInstanceOf(UserBreakException.class);
    }

}
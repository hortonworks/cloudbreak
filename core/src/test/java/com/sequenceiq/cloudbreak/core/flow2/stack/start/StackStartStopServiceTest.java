package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus.Value.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_STOP_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.metering.MeteringService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class StackStartStopServiceTest {

    private static final long STACK_ID = 1L;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private MetadataSetupService metadatSetupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private MeteringService meteringService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private StackStartStopService underTest;

    static Stream<Arguments> stackViewScenarios() {
        return Stream.concat(
                Arrays.stream(Status.values()).map(status -> Arguments.of(status.name(), stackViewWithStatus(status), isStopPossibleByStatus(status))),
                Stream.of(Arguments.of("Null stackview", null, false),
                        Arguments.of("Null stackview status", stackViewWithStatus(null), false))
        );
    }

    private static StackView stackViewWithStatus(Status status) {
        StackView stackView = spy(StackView.class);
        when(stackView.getId()).thenReturn(10L);
        when(stackView.getName()).thenReturn("stackview");
        when(stackView.getCloudPlatform()).thenReturn("platform");
        when(stackView.getStatus()).thenReturn(status);
        return stackView;
    }

    private static boolean isStopPossibleByStatus(Status status) {
        return status == STOP_REQUESTED ||
                status == STOP_IN_PROGRESS ||
                status == EXTERNAL_DATABASE_STOP_IN_PROGRESS ||
                status == EXTERNAL_DATABASE_STOP_FINISHED;
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("stackViewScenarios")
    void isStopPossibleTest(String testName, StackView stack, boolean expected) {
        boolean result = underTest.isStopPossible(stack);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void testFinishStackStop() throws TransactionService.TransactionExecutionException {
        doAnswer((Answer<Void>) invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
        CloudContext cloudContext = CloudContext.Builder.builder().build();
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(STACK_ID);

        underTest.finishStackStop(new StackStartStopContext(null, stack, new ArrayList<>(), cloudContext, new CloudCredential()),
                new StopInstancesResult(STACK_ID, new InstancesStatusResult(cloudContext, new ArrayList<>())));
        verify(meteringService, times(1)).unscheduleSync(eq(STACK_ID));
        verify(meteringService, times(1)).sendMeteringStatusChangeEventForStack(eq(STACK_ID), eq(STOPPED));
    }
}

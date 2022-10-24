package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_DISK_SPACE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_IMAGE_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeS3guardValidationEvent;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@RunWith(MockitoJUnitRunner.class)
public class ClusterUpgradeS3guardValidationHandlerTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ClusterUpgradeS3guardValidationHandler underTest;

    @Mock
    private StackService stackService;

    @Mock
    private EnvironmentClientService environmentService;

    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @Test
    public void testHandlerToCheckNonS3guardEnvironment() {
        when(stackService.findEnvironmentCrnByStackId(STACK_ID)).thenReturn("1");
        when(environmentService.getByCrn("1")).thenReturn(detailedEnvironmentResponse);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent());

        assertEquals(START_CLUSTER_UPGRADE_IMAGE_VALIDATION_EVENT.name(), nextFlowStepSelector.selector());
        verify(stackService).findEnvironmentCrnByStackId(STACK_ID);
        verify(environmentService).getByCrn("1");
    }

    @Test
    public void testHandlerToCheckS3guardEnvironment() {
        when(stackService.findEnvironmentCrnByStackId(STACK_ID)).thenReturn("1");
        S3GuardRequestParameters s3Parameters = new S3GuardRequestParameters();
        s3Parameters.setDynamoDbTableName("X");
        AwsEnvironmentParameters awsEnvironmentParameters = new AwsEnvironmentParameters();
        awsEnvironmentParameters.setS3guard(s3Parameters);
        detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setAws(awsEnvironmentParameters);
        when(environmentService.getByCrn("1")).thenReturn(detailedEnvironmentResponse);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent());

        verify(stackService).findEnvironmentCrnByStackId(STACK_ID);
        verify(environmentService).getByCrn("1");
        assertEquals(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.name(), nextFlowStepSelector.selector());
    }

    private HandlerEvent<ClusterUpgradeS3guardValidationEvent> createEvent() {
        return new HandlerEvent<>(new Event<>(new ClusterUpgradeS3guardValidationEvent(VALIDATE_DISK_SPACE_EVENT.selector(), STACK_ID, "1")));
    }
}
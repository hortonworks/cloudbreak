package com.sequenceiq.cloudbreak.cloud.template.compute;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

@ExtendWith(MockitoExtension.class)
class ResourceStopStartCallableTest {

    private static final String INSTANCE_ID = "aProperInstanceId";

    @Mock
    private ResourceStopStartCallablePayload payload;

    @Mock
    private ResourceBuilderContext context;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthenticatedContext auth;

    @Mock
    private CloudInstance cloudInstance;

    @Mock
    private ComputeResourceBuilder<ResourceBuilderContext> builder;

    private ResourceStopStartCallable underTest;

    @BeforeEach
    void setUp() {
        when(payload.getAuth()).thenReturn(auth);
        when(payload.getBuilder()).thenReturn(builder);
        when(payload.getContext()).thenReturn(context);
        when(payload.getInstances()).thenReturn(List.of(cloudInstance));
        when(cloudInstance.getInstanceId()).thenReturn(INSTANCE_ID);
        underTest = new ResourceStopStartCallable(payload);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testCallShouldThrowExceptionWhenTheUnderlyingBuilderFailsForACloudInstance(boolean startContext) {
        when(context.isBuild()).thenReturn(startContext);
        String expectedExceptionMessage;
        if (startContext) {
            when(builder.start(context, auth, cloudInstance)).thenThrow(new CloudConnectorException("The instance failed to start"));
            expectedExceptionMessage = "Starting of instance '" + INSTANCE_ID + "' failed, please retry the failed operation";
        } else {
            when(builder.stop(context, auth, cloudInstance)).thenThrow(new CloudConnectorException("The instance failed to stop"));
            expectedExceptionMessage = "Stopping of instance '" + INSTANCE_ID + "' failed, please retry the failed operation";
        }

        assertThrows(InstanceResourceStopStartException.class,
                () -> underTest.call(),
                expectedExceptionMessage);

        if (startContext) {
            verify(builder, times(1)).start(context, auth, cloudInstance);
        } else {
            verify(builder, times(1)).stop(context, auth, cloudInstance);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testCallShouldNotThrowExceptionAndReturnInstanceStatusWhenTheUnderlyingBuilderDoesNotFail(boolean startContext) {
        when(context.isBuild()).thenReturn(startContext);
        CloudVmInstanceStatus cloudVmInstanceStatus = mock(CloudVmInstanceStatus.class);
        if (startContext) {
            when(builder.start(context, auth, cloudInstance)).thenReturn(cloudVmInstanceStatus);
        } else {
            when(builder.stop(context, auth, cloudInstance)).thenReturn(cloudVmInstanceStatus);
        }

        List<CloudVmInstanceStatus> actualStatuses = assertDoesNotThrow(() -> underTest.call());

        if (startContext) {
            verify(builder, times(1)).start(context, auth, cloudInstance);
        } else {
            verify(builder, times(1)).stop(context, auth, cloudInstance);
        }
        Assertions.assertTrue(actualStatuses.contains(cloudVmInstanceStatus));
    }
}
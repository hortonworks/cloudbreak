package com.sequenceiq.distrox.v1.distrox.converter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.distrox.api.v1.distrox.model.sharedservice.SdxV1Request;

@ExtendWith(MockitoExtension.class)
public class SdxConverterTest {

    @InjectMocks
    private SdxConverter underTest;

    @Mock
    private StackService stackService;

    @Test
    public void testGetSharedServiceWhenSdxNullAndEnvironmentHasNotSdx() {
        String environmenName = "environmenName";
        when(stackService.getByEnvironmentCrnAndStackType(environmenName, StackType.DATALAKE)).thenReturn(Collections.emptyList());

        SharedServiceV4Request sdxRequest = underTest.getSharedService(null, environmenName);

        Assertions.assertNull(sdxRequest);
    }

    @Test
    public void testGetSharedServiceWhenSdxNullAndEnvironmentHasMoreThanOneSdx() {
        String environmenName = "environmenName";
        when(stackService.getByEnvironmentCrnAndStackType(environmenName, StackType.DATALAKE))
                .thenReturn(List.of(mock(StackStatusView.class), mock(StackStatusView.class)));

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.getSharedService(null, environmenName));
        Assertions.assertEquals(exception.getMessage(), "More than one Datalake attached to the environment. Please specify one.");
    }

    @Test
    public void testGetSharedServiceWhenSdxNullAndSdxInvIsRunning() {
        String environmenName = "environmenName";
        StackStatusView stack = mock(StackStatusView.class);
        when(stack.getName()).thenReturn("some-sdx");
        when(stack.getStatus()).thenReturn(getStatus(Status.AVAILABLE));
        when(stackService.getByEnvironmentCrnAndStackType(environmenName, StackType.DATALAKE)).thenReturn(List.of(stack));

        SharedServiceV4Request sdxRequest = underTest.getSharedService(null, environmenName);

        Assertions.assertEquals("some-sdx", sdxRequest.getDatalakeName());
    }

    @Test
    public void testGetSharedServiceWhenSdxNotAttachedToEnvironment() {
        String environmenName = "environmenName";
        StackStatusView stack = mock(StackStatusView.class);
        when(stack.getName()).thenReturn("some-sdx");
        when(stackService.getByEnvironmentCrnAndStackType(environmenName, StackType.DATALAKE)).thenReturn(List.of(stack));

        SdxV1Request sdxRequest = new SdxV1Request();
        sdxRequest.setName("other-sdx-name");

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.getSharedService(sdxRequest, environmenName));
        Assertions.assertEquals(exception.getMessage(), "The given Datalake not attached to the environment");
    }

    @Test
    public void testGetSharedServiceWhenSdxInvIsNotRunning() {
        String environmenName = "environmenName";
        StackStatusView stack = mock(StackStatusView.class);
        when(stack.getName()).thenReturn("some-sdx");
        when(stack.getStatus()).thenReturn(getStatus(Status.DELETE_FAILED));
        when(stackService.getByEnvironmentCrnAndStackType(environmenName, StackType.DATALAKE)).thenReturn(List.of(stack));

        SdxV1Request sdxRequest = new SdxV1Request();
        sdxRequest.setName("some-sdx");

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.getSharedService(sdxRequest, environmenName));
        Assertions.assertEquals(exception.getMessage(), "Datalake status is invalid. Current state is DELETE_FAILED instead of AVAILABLE");
    }

    private StackStatus getStatus(Status status) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(status);
        return stackStatus;
    }
}

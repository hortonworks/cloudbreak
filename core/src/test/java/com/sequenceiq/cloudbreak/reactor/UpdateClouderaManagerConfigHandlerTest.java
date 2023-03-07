package com.sequenceiq.cloudbreak.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateClouderaManagerConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateClouderaManagerConfigResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
public class UpdateClouderaManagerConfigHandlerTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private EventBus eventBus;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @InjectMocks
    private UpdateClouderaManagerConfigHandler underTest;

    private StackDto stackDto;

    private ClusterApi clusterApi;

    private StackVerticalScaleV4Request stackVerticalScaleV4Request;

    @BeforeEach
    void setUp() {
        stackDto = mock(StackDto.class);
        clusterApi = mock(ClusterApi.class);
        stackVerticalScaleV4Request = mock(StackVerticalScaleV4Request.class);
        doNothing().when(eventBus).notify(anyString(), any(Event.class));
    }

    @Test
    void testAcceptUpdateCMConfigSuccess() {
        Event<UpdateClouderaManagerConfigRequest> input = mock(Event.class);
        UpdateClouderaManagerConfigRequest request = mock(UpdateClouderaManagerConfigRequest.class);
        doReturn(request).when(input).getData();
        doReturn(stackVerticalScaleV4Request).when(request).getStackVerticalScaleV4Request();
        doReturn(1L).when(stackVerticalScaleV4Request).getStackId();
        doReturn(stackDto).when(stackDtoService).getById(1L);
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(stackDto);
        doReturn(new Promise<>()).when(request).getResult();
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        underTest.accept(input);
        verify(eventBus).notify(selectorCaptor.capture(), any(Event.class));
        assertEquals(UpdateClouderaManagerConfigResult.class.getSimpleName().toUpperCase(), selectorCaptor.getValue());
    }

    @Test
    void testAcceptUpdateCMConfigFailure() {
        Event<UpdateClouderaManagerConfigRequest> input = mock(Event.class);
        UpdateClouderaManagerConfigRequest request = mock(UpdateClouderaManagerConfigRequest.class);
        doReturn(request).when(input).getData();
        doReturn(stackVerticalScaleV4Request).when(request).getStackVerticalScaleV4Request();
        doReturn(1L).when(stackVerticalScaleV4Request).getStackId();
        doThrow(new RuntimeException("TEST")).when(stackDtoService).getById(1L);
        doReturn(new Promise<>()).when(request).getResult();
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        underTest.accept(input);
        verify(eventBus).notify(selectorCaptor.capture(), any(Event.class));
        assertEquals(UpdateClouderaManagerConfigResult.class.getSimpleName().toUpperCase()+"_ERROR", selectorCaptor.getValue());
    }
}

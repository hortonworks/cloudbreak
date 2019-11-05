package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.handler.EnvironmentUMSResourceDeleteHandler.INTERNAL_ACTOR_CRN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentTestConstants;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class EnvironmentUMSResourceDeleteHandlerTest {

    private static final String TEST_CRN = EnvironmentTestConstants.CRN;

    @InjectMocks
    private EnvironmentUMSResourceDeleteHandler underTest;

    @Mock
    private GrpcUmsClient umsClient;

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private Event<EnvironmentDto> environmentDtoEvent;

    @Mock
    private EnvironmentDto environmentDto;

    @Mock
    private VirtualGroupService virtualGroupService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new EnvironmentUMSResourceDeleteHandler(eventSender, environmentService, umsClient, virtualGroupService);
    }

    @Test
    public void testAccept() {
        // GIVEN
        given(environmentDtoEvent.getData()).willReturn(environmentDto);
        given(environmentDto.getResourceCrn()).willReturn(TEST_CRN);
        given(environmentService.findEnvironmentById(any())).willReturn(Optional.of(new Environment()));
        doNothing().when(umsClient).notifyResourceDeleted(INTERNAL_ACTOR_CRN, TEST_CRN, Optional.empty());
        doNothing().when(eventSender).sendEvent(any(EnvDeleteEvent.class), any());
        // WHEN
        underTest.accept(environmentDtoEvent);
        // THEN
        verify(environmentService, times(1)).findEnvironmentById(any());
        verify(umsClient, times(1)).notifyResourceDeleted(INTERNAL_ACTOR_CRN, TEST_CRN, Optional.empty());
        verify(eventSender, times(1)).sendEvent(any(EnvDeleteEvent.class), any());
    }

    @Test
    public void testAcceptWithoutEnvDtoCrn() {
        // GIVEN
        String crnFromQuery = TEST_CRN;
        Environment env = new Environment();
        env.setResourceCrn(crnFromQuery);
        given(environmentDtoEvent.getData()).willReturn(environmentDto);
        given(environmentDto.getResourceCrn()).willReturn(null);
        given(environmentService.findEnvironmentById(any())).willReturn(Optional.of(env));
        doNothing().when(umsClient).notifyResourceDeleted(INTERNAL_ACTOR_CRN, crnFromQuery, Optional.empty());
        doNothing().when(eventSender).sendEvent(any(EnvDeleteEvent.class), any());
        // WHEN
        underTest.accept(environmentDtoEvent);
        // THEN
        verify(environmentService, times(1)).findEnvironmentById(any());
        verify(umsClient, times(1)).notifyResourceDeleted(INTERNAL_ACTOR_CRN, crnFromQuery, Optional.empty());
        verify(eventSender, times(1)).sendEvent(any(EnvDeleteEvent.class), any());
    }

    @Test
    public void testAcceptWithException() {
        // GIVEN
        given(environmentDtoEvent.getData()).willReturn(environmentDto);
        given(environmentDto.getResourceCrn()).willReturn(TEST_CRN);
        given(environmentService.findEnvironmentById(any())).willReturn(Optional.of(new Environment()));
        doThrow(new UmsOperationException("ums exception")).when(umsClient).notifyResourceDeleted(INTERNAL_ACTOR_CRN, TEST_CRN, Optional.empty());
        doNothing().when(eventSender).sendEvent(any(EnvDeleteEvent.class), any());
        // WHEN
        underTest.accept(environmentDtoEvent);
        // THEN
        verify(environmentService, times(1)).findEnvironmentById(any());
        verify(umsClient, times(1)).notifyResourceDeleted(INTERNAL_ACTOR_CRN, TEST_CRN, Optional.empty());
        verify(eventSender, times(1)).sendEvent(any(EnvDeleteEvent.class), any());
    }
}

package com.sequenceiq.environment.environment.flow.deletion.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class EnvironmentUMSResourceDeleteHandlerTest {

    private static final String TEST_CRN = TestConstants.CRN;

    @InjectMocks
    private EnvironmentUMSResourceDeleteHandler underTest;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private Event<EnvironmentDeletionDto> environmentDtoEvent;

    @Mock
    private EnvironmentDeletionDto environmentDeletionDto;

    @Mock
    private EnvironmentDto environmentDto;

    @Mock
    private VirtualGroupService virtualGroupService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new EnvironmentUMSResourceDeleteHandler(eventSender, environmentService, ownerAssignmentService, virtualGroupService);
    }

    @Test
    public void testAccept() {
        // GIVEN
        given(environmentDtoEvent.getData()).willReturn(environmentDeletionDto);
        given(environmentDeletionDto.getEnvironmentDto()).willReturn(environmentDto);
        given(environmentDto.getResourceCrn()).willReturn(TEST_CRN);
        given(environmentService.findEnvironmentById(any())).willReturn(Optional.of(new Environment()));
        doNothing().when(ownerAssignmentService).notifyResourceDeleted(eq(TEST_CRN));
        // WHEN
        underTest.accept(environmentDtoEvent);
        // THEN
        verify(environmentService, times(1)).findEnvironmentById(any());
        verify(ownerAssignmentService, times(1)).notifyResourceDeleted(eq(TEST_CRN));
        verify(eventSender, times(1)).sendEvent(any(EnvDeleteEvent.class), any());
    }

    @Test
    public void testAcceptWithoutEnvDtoCrn() {
        // GIVEN
        String crnFromQuery = TEST_CRN;
        Environment env = new Environment();
        env.setResourceCrn(crnFromQuery);
        given(environmentDtoEvent.getData()).willReturn(environmentDeletionDto);
        given(environmentDeletionDto.getEnvironmentDto()).willReturn(environmentDto);
        given(environmentDto.getResourceCrn()).willReturn(null);
        given(environmentService.findEnvironmentById(any())).willReturn(Optional.of(env));
        doNothing().when(ownerAssignmentService).notifyResourceDeleted(eq(crnFromQuery));
        // WHEN
        underTest.accept(environmentDtoEvent);
        // THEN
        verify(environmentService, times(1)).findEnvironmentById(any());
        verify(ownerAssignmentService, times(1)).notifyResourceDeleted(eq(crnFromQuery));
        verify(eventSender, times(1)).sendEvent(any(EnvDeleteEvent.class), any());
    }

    @Test
    public void testAcceptWithException() {
        // GIVEN

        given(environmentDtoEvent.getData()).willReturn(environmentDeletionDto);
        given(environmentDeletionDto.getEnvironmentDto()).willReturn(environmentDto);
        given(environmentDto.getResourceCrn()).willReturn(TEST_CRN);
        given(environmentService.findEnvironmentById(any())).willReturn(Optional.of(new Environment()));
        doThrow(new UmsOperationException("ums exception")).when(ownerAssignmentService).notifyResourceDeleted(eq(TEST_CRN));
        // WHEN
        underTest.accept(environmentDtoEvent);
        // THEN
        verify(environmentService, times(1)).findEnvironmentById(any());
        verify(ownerAssignmentService, times(1)).notifyResourceDeleted(eq(TEST_CRN));
        verify(eventSender, times(1)).sendEvent(any(EnvDeleteEvent.class), any());
    }
}

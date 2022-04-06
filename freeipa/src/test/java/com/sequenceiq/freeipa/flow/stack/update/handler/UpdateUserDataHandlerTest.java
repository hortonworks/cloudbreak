package com.sequenceiq.freeipa.flow.stack.update.handler;

import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents.UPDATE_USERDATA_FAILED_EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateRequest;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateSuccess;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class UpdateUserDataHandlerTest {

    private static final long STACK_ID = 234L;

    @Mock
    private UserDataService userDataService;

    @Mock
    private Event<UserDataUpdateRequest> event;

    @Mock
    private EventBus eventBus;

    @Mock
    private UserDataUpdateRequest userDataUpdateRequest;

    @InjectMocks
    private UpdateUserDataHandler underTest;

    @BeforeEach
    void setUp() {
        when(event.getData()).thenReturn(userDataUpdateRequest);
        when(userDataUpdateRequest.getResourceId()).thenReturn(STACK_ID);
    }

    @Test
    void tunnelCcmV1ThenRegenerateUserData() {
        when(userDataUpdateRequest.getOldTunnel()).thenReturn(Tunnel.CCM);
        underTest.accept(event);
        verify(userDataService).regenerateUserData(STACK_ID);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(UserDataUpdateSuccess.class)), any(Event.class));
    }

    @Test
    void tunnelCcmV1ThenRegenerateFails() {
        when(userDataUpdateRequest.getOldTunnel()).thenReturn(Tunnel.CCM);
        doThrow(new IllegalStateException("failure")).when(userDataService).regenerateUserData(any());
        underTest.accept(event);
        verify(userDataService).regenerateUserData(STACK_ID);
        verify(eventBus).notify(eq(UPDATE_USERDATA_FAILED_EVENT.event()), any(Event.class));
    }

    @Test
    void tunnelCcmV2ThenUpdateFlagOnly() {
        when(userDataUpdateRequest.getOldTunnel()).thenReturn(Tunnel.CCMV2);
        underTest.accept(event);
        verify(userDataService).updateJumpgateFlagOnly(STACK_ID);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(UserDataUpdateSuccess.class)), any(Event.class));
    }

    @Test
    void tunnelCcmV2ThenUpdateFails() {
        when(userDataUpdateRequest.getOldTunnel()).thenReturn(Tunnel.CCMV2);
        doThrow(new IllegalStateException("failure")).when(userDataService).updateJumpgateFlagOnly(any());
        underTest.accept(event);
        verify(userDataService).updateJumpgateFlagOnly(STACK_ID);
        verify(eventBus).notify(eq(UPDATE_USERDATA_FAILED_EVENT.event()), any(Event.class));
    }

    @EnumSource(value = Tunnel.class, names = { "CCM", "CCMV2" }, mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void notValidTunnelFails(Tunnel tunnel) {
        when(userDataUpdateRequest.getOldTunnel()).thenReturn(tunnel);
        underTest.accept(event);
        verifyNoInteractions(userDataService);
        verify(eventBus).notify(eq(UPDATE_USERDATA_FAILED_EVENT.event()), any(Event.class));
    }
}

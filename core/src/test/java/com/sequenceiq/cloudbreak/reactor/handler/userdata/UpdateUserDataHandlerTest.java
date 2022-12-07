package com.sequenceiq.cloudbreak.reactor.handler.userdata;

import static com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.UpdateUserDataEvents.UPDATE_USERDATA_FAILED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateSuccess;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class UpdateUserDataHandlerTest {

    private static final long STACK_ID = 1L;

    @Mock
    private UserDataService userDataService;

    @InjectMocks
    private UpdateUserDataHandler underTest;

    @Mock
    private Event<UserDataUpdateRequest> event;

    @Mock
    private UserDataUpdateRequest request;

    @BeforeEach
    void setUp() {
        lenient().when(event.getData()).thenReturn(request);
        lenient().when(request.getResourceId()).thenReturn(STACK_ID);
    }

    @Test
    void defaultFailureEvent() {
        Exception cause = new Exception("cause");

        Selectable result = underTest.defaultFailureEvent(STACK_ID, cause, event);

        assertFailed(result, cause);
    }

    @ParameterizedTest
    @MethodSource("tunnelAndProxyParams")
    void doAcceptSuccess(Tunnel oldTunnel, boolean modifyProxyConfig) {
        lenient().when(request.getOldTunnel()).thenReturn(oldTunnel);
        lenient().when(request.isModifyProxyConfig()).thenReturn(modifyProxyConfig);

        Selectable result = underTest.doAccept(new HandlerEvent<>(event));

        assertSuccess(result);
        verify(userDataService, times(oldTunnel != null ? 1 : 0)).updateJumpgateFlagOnly(STACK_ID);
        verify(userDataService, times(modifyProxyConfig ? 1 : 0)).updateProxyConfig(STACK_ID);
    }

    @ParameterizedTest
    @MethodSource("tunnelAndProxyParams")
    void doAcceptFailure(Tunnel oldTunnel, boolean modifyProxyConfig) throws Exception {
        lenient().when(request.getOldTunnel()).thenReturn(oldTunnel);
        lenient().when(request.isModifyProxyConfig()).thenReturn(modifyProxyConfig);

        RuntimeException ccmCause = new RuntimeException("ccmCause");
        lenient().doThrow(ccmCause).when(userDataService).updateJumpgateFlagOnly(STACK_ID);
        RuntimeException proxyCause = new RuntimeException("proxyCause");
        lenient().doThrow(proxyCause).when(userDataService).updateProxyConfig(STACK_ID);

        Selectable result = underTest.doAccept(new HandlerEvent<>(event));

        if (oldTunnel == null && !modifyProxyConfig) {
            assertSuccess(result);
        } else {
            assertFailed(result, oldTunnel != null ? ccmCause : proxyCause);
        }
    }

    private static Stream<Arguments> tunnelAndProxyParams() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of(Tunnel.CCM, false),
                Arguments.of(null, true),
                Arguments.of(Tunnel.CCM, true)
        );
    }

    private static void assertSuccess(Selectable result) {
        assertThat(result)
                .isInstanceOf(UserDataUpdateSuccess.class)
                .extracting(UserDataUpdateSuccess.class::cast)
                .returns(STACK_ID, UserDataUpdateSuccess::getResourceId);
    }

    private static void assertFailed(Selectable result, Exception cause) {
        assertThat(result)
                .isInstanceOf(UserDataUpdateFailed.class)
                .extracting(UserDataUpdateFailed.class::cast)
                .returns(STACK_ID, UserDataUpdateFailed::getResourceId)
                .returns(cause, UserDataUpdateFailed::getException)
                .returns(UPDATE_USERDATA_FAILED_EVENT.event(), UserDataUpdateFailed::selector);
    }

}

package com.sequenceiq.datalake.flow.salt.rotatepassword.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordFailureResponse;
import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordSuccessResponse;
import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordWaitRequest;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordWaitHandlerTest {

    private static final String USER_ID = "user-id";

    private static final long SDX_ID = 1L;

    private static final HandlerEvent<RotateSaltPasswordWaitRequest> EVENT =
            new HandlerEvent<>(new Event<>(new RotateSaltPasswordWaitRequest(SDX_ID, USER_ID)));

    @Mock
    private SdxWaitService sdxWaitService;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxCluster sdxCluster;

    @InjectMocks
    private RotateSaltPasswordWaitHandler underTest;

    @BeforeEach
    void setUp() {
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
    }

    @Test
    void testSuccess() {
        Selectable result = underTest.doAccept(EVENT);

        assertThat(result)
                .isInstanceOf(RotateSaltPasswordSuccessResponse.class)
                .extracting(RotateSaltPasswordSuccessResponse.class::cast)
                .returns(SDX_ID, RotateSaltPasswordSuccessResponse::getResourceId)
                .returns(USER_ID, RotateSaltPasswordSuccessResponse::getUserId);
        verify(sdxWaitService).waitForCloudbreakFlow(eq(sdxCluster), any(), eq("Rotating SaltStack user password"));
    }

    @Test
    void testFailure() {
        SdxWaitException sdxWaitException = new SdxWaitException("message", new Throwable());
        doThrow(sdxWaitException).when(sdxWaitService).waitForCloudbreakFlow(any(), any(), anyString());

        Selectable result = underTest.doAccept(EVENT);

        assertThat(result)
                .isInstanceOf(RotateSaltPasswordFailureResponse.class)
                .extracting(RotateSaltPasswordFailureResponse.class::cast)
                .returns(SDX_ID, RotateSaltPasswordFailureResponse::getResourceId)
                .returns(USER_ID, RotateSaltPasswordFailureResponse::getUserId)
                .returns(sdxWaitException, RotateSaltPasswordFailureResponse::getException);
        verify(sdxWaitService).waitForCloudbreakFlow(eq(sdxCluster), any(), eq("Rotating SaltStack user password"));
    }
}

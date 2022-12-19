package com.sequenceiq.redbeams.flow.redbeams.start.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.event.CertRotateInRedbeamsRequest;
import com.sequenceiq.redbeams.flow.redbeams.start.event.CertRotateInRedbeamsSuccess;
import com.sequenceiq.redbeams.service.stack.DBStackUpdater;

@ExtendWith(MockitoExtension.class)
public class CertRotateInRedbeamsHandlerTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private CertRotateInRedbeamsHandler underTest;

    @Mock
    private DBStackUpdater dbStackUpdater;

    @Mock
    private CloudContext cloudContext;

    @Test
    public void testDoAccept() {
        when(cloudContext.getId()).thenReturn(STACK_ID);

        CertRotateInRedbeamsRequest request = new CertRotateInRedbeamsRequest(cloudContext);
        Event<CertRotateInRedbeamsRequest> event = new Event<>(request);
        HandlerEvent<CertRotateInRedbeamsRequest> handlerEvent = new HandlerEvent<>(event);

        Selectable selectable = underTest.doAccept(handlerEvent);
        Assertions.assertThat(selectable.getClass()).isEqualTo(CertRotateInRedbeamsSuccess.class);
        Assertions.assertThat(selectable.selector()).isEqualTo("CERTROTATEINREDBEAMSSUCCESS");
        verify(dbStackUpdater).updateSslConfig(STACK_ID);
    }
}

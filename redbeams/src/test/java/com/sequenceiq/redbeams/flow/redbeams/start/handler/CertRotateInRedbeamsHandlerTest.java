package com.sequenceiq.redbeams.flow.redbeams.start.handler;

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

@ExtendWith(MockitoExtension.class)
public class CertRotateInRedbeamsHandlerTest {

    @InjectMocks
    private CertRotateInRedbeamsHandler underTest;

    @Mock
    private CloudContext cloudContext;

    @Test
    public void testDoAccept() {
        CertRotateInRedbeamsRequest request = new CertRotateInRedbeamsRequest(cloudContext);
        Event<CertRotateInRedbeamsRequest> event = new Event<>(request);
        HandlerEvent<CertRotateInRedbeamsRequest> handlerEvent = new HandlerEvent<>(event);
        Selectable selectable = underTest.doAccept(handlerEvent);
        Assertions.assertThat(selectable.getClass()).isEqualTo(CertRotateInRedbeamsSuccess.class);
        Assertions.assertThat(selectable.selector()).isEqualTo("CERTROTATEINREDBEAMSSUCCESS");
    }
}

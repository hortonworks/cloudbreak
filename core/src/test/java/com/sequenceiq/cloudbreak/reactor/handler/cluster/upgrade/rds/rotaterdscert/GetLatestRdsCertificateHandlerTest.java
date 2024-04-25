package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.rotaterdscert;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert.RotateRdsCertificateEvent.GET_LATEST_RDS_CERTIFICATE_FINISHED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert.RotateRdsCertificateService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.GetLatestRdsCertificateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.GetLatestRdsCertificateResult;

@ExtendWith(MockitoExtension.class)
class GetLatestRdsCertificateHandlerTest {

    private static final long STACK_ID = 234L;

    @Mock
    private RotateRdsCertificateService rotateRdsCertificateService;

    @Mock
    private EventBus eventBus;

    @Captor
    private ArgumentCaptor<Event<GetLatestRdsCertificateResult>> eventCaptor;

    @InjectMocks
    private GetLatestRdsCertificateHandler underTest;

    private Event<GetLatestRdsCertificateRequest> event;

    @BeforeEach
    void setUp() {
        GetLatestRdsCertificateRequest request = new GetLatestRdsCertificateRequest(STACK_ID);
        event = new Event<>(request);
    }

    @Test
    void getLatestRdsCertificate() {
        underTest.accept(event);
        verify(rotateRdsCertificateService).getLatestRdsCertificate(STACK_ID);
        verify(eventBus).notify(eq(GET_LATEST_RDS_CERTIFICATE_FINISHED_EVENT.event()), eventCaptor.capture());
        Event<GetLatestRdsCertificateResult> eventResult = eventCaptor.getValue();
        assertThat(eventResult.getData().selector()).isEqualTo(GET_LATEST_RDS_CERTIFICATE_FINISHED_EVENT.event());
        assertThat(eventResult.getData().getResourceId()).isEqualTo(STACK_ID);
    }

}

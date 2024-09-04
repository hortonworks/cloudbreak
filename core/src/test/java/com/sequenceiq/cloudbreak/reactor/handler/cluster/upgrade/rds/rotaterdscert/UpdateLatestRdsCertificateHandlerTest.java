package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.rotaterdscert;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType.ROTATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.UPDATE_TO_LATEST_RDS_CERTIFICATE_FINISHED_EVENT;
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

import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.rotate.RotateRdsCertificateService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.UpdateLatestRdsCertificateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.UpdateLatestRdsCertificateResult;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.rotate.UpdateLatestRdsCertificateHandler;

@ExtendWith(MockitoExtension.class)
class UpdateLatestRdsCertificateHandlerTest {

    private static final long STACK_ID = 234L;

    @Mock
    private RotateRdsCertificateService rotateRdsCertificateService;

    @Mock
    private EventBus eventBus;

    @Captor
    private ArgumentCaptor<Event<UpdateLatestRdsCertificateResult>> eventCaptor;

    @InjectMocks
    private UpdateLatestRdsCertificateHandler underTest;

    private Event<UpdateLatestRdsCertificateRequest> event;

    @BeforeEach
    void setUp() {
        UpdateLatestRdsCertificateRequest request = new UpdateLatestRdsCertificateRequest(STACK_ID, ROTATE);
        event = new Event<>(request);
    }

    @Test
    void updateLatestRdsCertificate() {
        underTest.accept(event);
        verify(rotateRdsCertificateService).updateLatestRdsCertificate(STACK_ID);
        verify(eventBus).notify(eq(UPDATE_TO_LATEST_RDS_CERTIFICATE_FINISHED_EVENT.event()), eventCaptor.capture());
        Event<UpdateLatestRdsCertificateResult> eventResult = eventCaptor.getValue();
        assertThat(eventResult.getData().selector()).isEqualTo(UPDATE_TO_LATEST_RDS_CERTIFICATE_FINISHED_EVENT.event());
        assertThat(eventResult.getData().getResourceId()).isEqualTo(STACK_ID);
    }

}

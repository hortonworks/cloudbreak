package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.rotaterdscert;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType.ROTATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES_FINISHED_EVENT;
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
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateCheckPrerequisitesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateCheckPrerequisitesResult;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.rotate.CheckRotateRdsCertificatePrerequisitesHandler;

@ExtendWith(MockitoExtension.class)
class CheckRotateRdsCertificatePrerequisitesHandlerTest {

    private static final long STACK_ID = 234L;

    @Mock
    private RotateRdsCertificateService rotateRdsCertificateService;

    @Mock
    private EventBus eventBus;

    @Captor
    private ArgumentCaptor<Event<RotateRdsCertificateCheckPrerequisitesResult>> eventCaptor;

    @InjectMocks
    private CheckRotateRdsCertificatePrerequisitesHandler underTest;

    private Event<RotateRdsCertificateCheckPrerequisitesRequest> event;

    @BeforeEach
    void setUp() {
        RotateRdsCertificateCheckPrerequisitesRequest request = new RotateRdsCertificateCheckPrerequisitesRequest(STACK_ID, ROTATE);
        event = new Event<>(request);
    }

    @Test
    void checkPrerequisites() {
        underTest.accept(event);
        verify(rotateRdsCertificateService).checkPrerequisites(STACK_ID, ROTATE);
        verify(eventBus).notify(eq(ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES_FINISHED_EVENT.event()), eventCaptor.capture());
        Event<RotateRdsCertificateCheckPrerequisitesResult> eventResult = eventCaptor.getValue();
        assertThat(eventResult.getData().selector()).isEqualTo(ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES_FINISHED_EVENT.event());
        assertThat(eventResult.getData().getResourceId()).isEqualTo(STACK_ID);
    }

}

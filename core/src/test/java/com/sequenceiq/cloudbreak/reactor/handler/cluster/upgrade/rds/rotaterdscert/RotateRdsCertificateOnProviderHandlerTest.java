package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.rotaterdscert;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType.ROTATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.ROTATE_RDS_CERTIFICATE_ON_PROVIDER_FINISHED_EVENT;
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
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateOnProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateOnProviderResult;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.rotate.RotateRdsCertificateOnProviderHandler;

@ExtendWith(MockitoExtension.class)
class RotateRdsCertificateOnProviderHandlerTest {

    private static final long STACK_ID = 234L;

    @Mock
    private RotateRdsCertificateService rotateRdsCertificateService;

    @Mock
    private EventBus eventBus;

    @Captor
    private ArgumentCaptor<Event<RotateRdsCertificateOnProviderResult>> eventCaptor;

    @InjectMocks
    private RotateRdsCertificateOnProviderHandler underTest;

    private Event<RotateRdsCertificateOnProviderRequest> event;

    @BeforeEach
    void setUp() {
        RotateRdsCertificateOnProviderRequest request = new RotateRdsCertificateOnProviderRequest(STACK_ID, ROTATE);
        event = new Event<>(request);
    }

    @Test
    void rotateRdsCertificateOnProvider() {
        underTest.accept(event);
        verify(rotateRdsCertificateService).rotateOnProvider(STACK_ID);
        verify(eventBus).notify(eq(ROTATE_RDS_CERTIFICATE_ON_PROVIDER_FINISHED_EVENT.event()), eventCaptor.capture());
        Event<RotateRdsCertificateOnProviderResult> eventResult = eventCaptor.getValue();
        assertThat(eventResult.getData().selector()).isEqualTo(ROTATE_RDS_CERTIFICATE_ON_PROVIDER_FINISHED_EVENT.event());
        assertThat(eventResult.getData().getResourceId()).isEqualTo(STACK_ID);
    }
}

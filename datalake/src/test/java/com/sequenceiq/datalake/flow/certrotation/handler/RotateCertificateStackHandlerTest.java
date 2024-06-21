package com.sequenceiq.datalake.flow.certrotation.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.certrotation.event.RotateCertificateFailedEvent;
import com.sequenceiq.datalake.flow.certrotation.event.RotateCertificateStackRequest;
import com.sequenceiq.datalake.flow.certrotation.event.RotateCertificateSuccessEvent;
import com.sequenceiq.datalake.service.rotation.certificate.SdxDatabaseCertificateRotationService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandlerTestSupport;

@ExtendWith(MockitoExtension.class)
class RotateCertificateStackHandlerTest {

    @Mock
    private SdxDatabaseCertificateRotationService certificateRotationService;

    @Mock
    private EventBus eventBus;

    @Mock
    private SdxService sdxService;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<Event<?>> eventCaptor;

    @InjectMocks
    private RotateCertificateStackHandler underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "sleepTimeInSec", 1);
        ReflectionTestUtils.setField(underTest, "durationInMinutes", 2);
        lenient().doAnswer(i -> null).when(eventBus).notify(keyCaptor.capture(), eventCaptor.capture());
    }

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("RotateCertificateStackRequest");
    }

    @Test
    void defaultFailureEvent() {
        Selectable failureEvent = underTest.defaultFailureEvent(
                1L,
                new Exception("error"),
                new Event<>(new RotateCertificateStackRequest(1L, "user")));
        assertThat(failureEvent.selector()).isEqualTo("RotateCertificateFailedEvent");
    }

    @Test
    void acceptSuccess() {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.getById(any())).thenReturn(sdxCluster);
        RotateCertificateStackRequest request = new RotateCertificateStackRequest(123L, "user");
        PollingConfig expectedPollingConfig = new PollingConfig(1, TimeUnit.SECONDS, 2, TimeUnit.MINUTES);
        Event.Headers headers = new Event.Headers();
        Event<RotateCertificateStackRequest> event = new Event<>(headers, request);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        RotateCertificateSuccessEvent successEvent = new RotateCertificateSuccessEvent(123L, "user");

        RecursiveComparisonConfiguration config = RecursiveComparisonConfiguration.builder()
                .withIgnoredFields("accepted")
                .build();
        assertThat(selectable).usingRecursiveComparison(config).isEqualTo(successEvent);
        verify(certificateRotationService).initAndWaitForStackCertificateRotation(eq(sdxCluster), refEq(expectedPollingConfig));
    }

    @ParameterizedTest
    @ValueSource(classes = { UserBreakException.class, PollerStoppedException.class, PollerException.class })
    void acceptWithExceptions(Class<? extends Exception> errorClass)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        RotateCertificateStackRequest request = new RotateCertificateStackRequest(123L, "user");
        Event.Headers headers = new Event.Headers();
        Event<RotateCertificateStackRequest> event = new Event<>(headers, request);
        when(sdxService.getById(any())).thenReturn(getSdxCluster());

        doThrow(errorClass).when(certificateRotationService).initAndWaitForStackCertificateRotation(any(SdxCluster.class), any(PollingConfig.class));
        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        Exception error = errorClass.getDeclaredConstructor(String.class).newInstance("error");
        RotateCertificateFailedEvent failedEvent = new RotateCertificateFailedEvent(123L, "user", error);

        RecursiveComparisonConfiguration config = RecursiveComparisonConfiguration.builder()
                .withIgnoredFields("accepted")
                .build();
        assertThat(selectable).usingRecursiveComparison(config).isEqualTo(failedEvent);
    }

    private SdxCluster getSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        return sdxCluster;
    }

}

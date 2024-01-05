package com.sequenceiq.datalake.flow.refresh.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowEvent;
import com.sequenceiq.datalake.flow.refresh.event.DatahubRefreshFailedEvent;
import com.sequenceiq.datalake.flow.refresh.event.DatahubRefreshWaitEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.refresh.SdxRefreshService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandlerTestSupport;

@ExtendWith(MockitoExtension.class)
class DatahubRefreshHandlerTest {

    private static final long SDX_ID = 1L;

    @Mock
    private SdxRefreshService sdxRefreshService;

    @Mock
    private EventBus eventBus;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<Event<?>> eventCaptor;

    @InjectMocks
    private DatahubRefreshWaitHandler underTest;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(underTest, "sleepTimeInSec", 1);
        ReflectionTestUtils.setField(underTest, "durationInMinutes", 2);
        lenient().doAnswer(i -> null).when(eventBus).notify(keyCaptor.capture(), eventCaptor.capture());
    }

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("DATAHUBREFRESHWAITEVENT");
    }

    @Test
    void defaultFailureEvent() {
        Selectable failureEvent = underTest.defaultFailureEvent(SDX_ID, new Exception("error"),
                new Event<>(new DatahubRefreshWaitEvent(SDX_ID, "user")));
        assertThat(failureEvent.selector()).isEqualTo("DATAHUBREFRESHFAILEDEVENT");
    }

    @Test
    void acceptSuccess() {
        DatahubRefreshWaitEvent request = new DatahubRefreshWaitEvent(SDX_ID, "user");
        PollingConfig expectedPollingConfig = new PollingConfig(1, TimeUnit.SECONDS, 2, TimeUnit.MINUTES);
        Event.Headers headers = new Event.Headers();
        Event<DatahubRefreshWaitEvent> event = new Event<>(headers, request);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        assertEquals(selectable.selector(), DatahubRefreshFlowEvent.DATAHUB_REFRESH_FINISHED_EVENT.selector());
        verify(sdxRefreshService).waitCloudbreakCluster(eq(SDX_ID), refEq(expectedPollingConfig));
    }

    @ParameterizedTest
    @ValueSource(classes = {UserBreakException.class, PollerStoppedException.class, PollerException.class})
    void acceptWithExceptions(Class<? extends Exception> errorClass)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        DatahubRefreshWaitEvent request = new DatahubRefreshWaitEvent(SDX_ID, "user");
        Event.Headers headers = new Event.Headers();
        Event<DatahubRefreshWaitEvent> event = new Event<>(headers, request);

        doThrow(errorClass).when(sdxRefreshService).waitCloudbreakCluster(eq(SDX_ID), any(PollingConfig.class));
        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        Exception error = errorClass.getDeclaredConstructor(String.class).newInstance("error");
        DatahubRefreshFailedEvent failedEvent = new DatahubRefreshFailedEvent(SDX_ID, "user", error);

        assertThat(selectable)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder().withIgnoredFields("accepted").build())
                .isEqualTo(failedEvent);
    }

}

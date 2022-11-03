package com.sequenceiq.datalake.flow.datalake.upgrade.preparation.handler;

import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_SUCCESS_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event.DatalakeUpgradePreparationFailedEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event.DatalakeUpgradePreparationWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DatalakeUpgradePreparationWaitHandlerTest {

    private static final String USER_ID = "userId";

    private static final long SDX_ID = 1L;

    @Mock
    private SdxUpgradeService upgradeService;

    @InjectMocks
    private DatalakeUpgradePreparationWaitHandler underTest;

    @Test
    public void testSelector() {
        assertEquals(EventSelectorUtil.selector(DatalakeUpgradePreparationWaitRequest.class), underTest.selector());
    }

    @Test
    public void testDefaultFailureEvent() {
        Exception exception = new Exception();

        DatalakeUpgradePreparationFailedEvent result = (DatalakeUpgradePreparationFailedEvent) underTest.defaultFailureEvent(SDX_ID, exception,
                new Event<>(new DatalakeUpgradePreparationWaitRequest(SDX_ID, USER_ID)));

        assertEquals(SDX_ID, result.getResourceId());
        assertEquals(USER_ID, result.getUserId());
        assertEquals(exception, result.getException());
    }

    @Test
    public void testSuccessfulPolling() {
        DatalakeUpgradePreparationWaitRequest datalakeUpgradePreparationWaitRequest = new DatalakeUpgradePreparationWaitRequest(SDX_ID, USER_ID);

        SdxEvent result = (SdxEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(datalakeUpgradePreparationWaitRequest)));

        verify(upgradeService).waitCloudbreakFlow(eq(SDX_ID), any(PollingConfig.class), eq("Cluster Upgrade preparation"));
        assertEquals(DATALAKE_UPGRADE_PREPARATION_SUCCESS_EVENT.event(), result.selector());
        assertEquals(SDX_ID, result.getResourceId());
        assertEquals(USER_ID, result.getUserId());
    }

    static Stream<Arguments> pollerExceptions() {
        return Stream.of(
                Arguments.of(new UserBreakException()),
                Arguments.of(new PollerException())
                );
    }

    @ParameterizedTest
    @MethodSource("pollerExceptions")
    public void testUserBreakExceptionWhilePolling(Exception e) {
        DatalakeUpgradePreparationWaitRequest datalakeUpgradePreparationWaitRequest = new DatalakeUpgradePreparationWaitRequest(SDX_ID, USER_ID);
        doThrow(e)
                .when(upgradeService).waitCloudbreakFlow(eq(SDX_ID), any(PollingConfig.class), eq("Cluster Upgrade preparation"));

        DatalakeUpgradePreparationFailedEvent result =
                (DatalakeUpgradePreparationFailedEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(datalakeUpgradePreparationWaitRequest)));

        assertEquals(e, result.getException());
        assertEquals(SDX_ID, result.getResourceId());
        assertEquals(USER_ID, result.getUserId());
    }

    @Test
    public void testUserBreakExceptionWhilePolling() {
        DatalakeUpgradePreparationWaitRequest datalakeUpgradePreparationWaitRequest = new DatalakeUpgradePreparationWaitRequest(SDX_ID, USER_ID);
        doThrow(new PollerStoppedException())
                .when(upgradeService).waitCloudbreakFlow(eq(SDX_ID), any(PollingConfig.class), eq("Cluster Upgrade preparation"));

        DatalakeUpgradePreparationFailedEvent result =
                (DatalakeUpgradePreparationFailedEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(datalakeUpgradePreparationWaitRequest)));

        assertEquals("Datalake upgrade preparation timed out after 0 minutes", result.getException().getMessage());
        assertEquals(PollerStoppedException.class, result.getException().getClass());
        assertEquals(SDX_ID, result.getResourceId());
        assertEquals(USER_ID, result.getUserId());
    }
}
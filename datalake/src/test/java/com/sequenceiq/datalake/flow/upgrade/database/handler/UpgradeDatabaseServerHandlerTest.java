package com.sequenceiq.datalake.flow.upgrade.database.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerSuccessEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.UpgradeDatabaseServerRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.poller.PollerRunner;
import com.sequenceiq.datalake.service.upgrade.database.SdxDatabaseServerUpgradeService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class UpgradeDatabaseServerHandlerTest {

    private static final long SLEEP_TIME_SECONDS = 17L;

    private static final long RESOURCE_ID = 2L;

    private static final long SDX_ID = 2L;

    private static final long DURATION_MINUTES = 20L;

    @Mock
    private UpgradeDatabaseServerWaitParametersService waitParametersService;

    @Mock
    private SdxDatabaseServerUpgradeService sdxDatabaseServerUpgradeService;

    @Mock
    private SdxService sdxService;

    @Mock
    private PollerRunner pollerRunner;

    @InjectMocks
    private UpgradeDatabaseServerHandler underTest;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(UpgradeDatabaseServerRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception();
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        Event<UpgradeDatabaseServerRequest> event = setupEvent(targetMajorVersion);

        Selectable selectable = underTest.defaultFailureEvent(RESOURCE_ID, e, event);

        assertEquals(EventSelectorUtil.selector(SdxUpgradeDatabaseServerFailedEvent.class), selectable.getSelector());
    }

    @Test
    void testDoAcceptWhenSuccess() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        HandlerEvent<UpgradeDatabaseServerRequest> event = setupHandlerEvent(targetMajorVersion);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        new PollerRunnerMock().mockSuccess(pollerRunner);
        when(waitParametersService.getDurationInMinutes()).thenReturn(DURATION_MINUTES);
        when(waitParametersService.getSleepTimeInSec()).thenReturn(SLEEP_TIME_SECONDS);

        Selectable selectable = underTest.doAccept(event);

        assertEquals(EventSelectorUtil.selector(SdxUpgradeDatabaseServerSuccessEvent.class), selectable.getSelector());
        verify(sdxDatabaseServerUpgradeService).initUpgradeInCb(sdxCluster, targetMajorVersion);
        ArgumentCaptor<PollingConfig> pollingConfigCaptor = ArgumentCaptor.forClass(PollingConfig.class);
        verify(sdxDatabaseServerUpgradeService).waitDatabaseUpgradeInCb(eq(sdxCluster), pollingConfigCaptor.capture());
        PollingConfig pollingConfig = pollingConfigCaptor.getValue();
        assertEquals(SLEEP_TIME_SECONDS, pollingConfig.getSleepTime());
        assertEquals(DURATION_MINUTES, pollingConfig.getDuration());
        assertEquals(TimeUnit.SECONDS, pollingConfig.getSleepTimeUnit());
        assertEquals(TimeUnit.MINUTES, pollingConfig.getDurationTimeUnit());
        verify(sdxDatabaseServerUpgradeService).updateDatabaseServerEngineVersion(sdxCluster);
    }

    @Test
    void testDoAcceptWhenFailure() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        HandlerEvent<UpgradeDatabaseServerRequest> event = setupHandlerEvent(targetMajorVersion);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        new PollerRunnerMock().mockError(pollerRunner);
        when(waitParametersService.getDurationInMinutes()).thenReturn(DURATION_MINUTES);
        when(waitParametersService.getSleepTimeInSec()).thenReturn(SLEEP_TIME_SECONDS);

        Selectable selectable = underTest.doAccept(event);

        assertEquals(EventSelectorUtil.selector(SdxUpgradeDatabaseServerFailedEvent.class), selectable.getSelector());
        verify(sdxDatabaseServerUpgradeService).initUpgradeInCb(sdxCluster, targetMajorVersion);
        ArgumentCaptor<PollingConfig> pollingConfigCaptor = ArgumentCaptor.forClass(PollingConfig.class);
        verify(sdxDatabaseServerUpgradeService).waitDatabaseUpgradeInCb(eq(sdxCluster), pollingConfigCaptor.capture());
        PollingConfig pollingConfig = pollingConfigCaptor.getValue();
        assertEquals(SLEEP_TIME_SECONDS, pollingConfig.getSleepTime());
        assertEquals(DURATION_MINUTES, pollingConfig.getDuration());
        assertEquals(TimeUnit.SECONDS, pollingConfig.getSleepTimeUnit());
        assertEquals(TimeUnit.MINUTES, pollingConfig.getDurationTimeUnit());
    }

    private HandlerEvent<UpgradeDatabaseServerRequest> setupHandlerEvent(TargetMajorVersion targetMajorVersion) {
        return new HandlerEvent<>(setupEvent(targetMajorVersion));
    }

    private Event<UpgradeDatabaseServerRequest> setupEvent(TargetMajorVersion targetMajorVersion) {
        UpgradeDatabaseServerRequest request = new UpgradeDatabaseServerRequest(SDX_ID, "userId", targetMajorVersion);
        return new Event<>(request);
    }

}

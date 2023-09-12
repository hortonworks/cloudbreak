package com.sequenceiq.datalake.flow.upgrade.database.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerSuccessEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.UpgradeDatabaseServerRequest;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.upgrade.database.SdxDatabaseServerUpgradeService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class SdxUpgradeDatabaseServerHandlerTest {

    private static final long RESOURCE_ID = 2L;

    private static final long SDX_ID = 2L;

    @Mock
    private SdxDatabaseServerUpgradeService sdxDatabaseServerUpgradeService;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxUpgradeDatabaseServerHandler underTest;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(UpgradeDatabaseServerRequest.class), underTest.selector());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testDefaultFailureEvent(boolean forced) {
        Exception e = new Exception();
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        Event<UpgradeDatabaseServerRequest> event = setupEvent(targetMajorVersion, forced);

        Selectable selectable = underTest.defaultFailureEvent(RESOURCE_ID, e, event);

        assertEquals(EventSelectorUtil.selector(SdxUpgradeDatabaseServerFailedEvent.class), selectable.getSelector());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testDoAccept(boolean forced) {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        HandlerEvent<UpgradeDatabaseServerRequest> event = setupHandlerEvent(targetMajorVersion, forced);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);

        Selectable nextEvent = underTest.doAccept(event);

        assertEquals(nextEvent.getSelector(), EventSelectorUtil.selector(SdxUpgradeDatabaseServerSuccessEvent.class));
        verify(sdxDatabaseServerUpgradeService).initUpgradeInCb(sdxCluster, targetMajorVersion, forced);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testDoAcceptWhenInitUpgradeInCbThrows(boolean forced) {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        HandlerEvent<UpgradeDatabaseServerRequest> event = setupHandlerEvent(targetMajorVersion, forced);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        doThrow(RuntimeException.class).when(sdxDatabaseServerUpgradeService).initUpgradeInCb(sdxCluster, targetMajorVersion, forced);

        Selectable nextEvent = underTest.doAccept(event);

        assertEquals(nextEvent.getSelector(), EventSelectorUtil.selector(SdxUpgradeDatabaseServerFailedEvent.class));
    }

    private HandlerEvent<UpgradeDatabaseServerRequest> setupHandlerEvent(TargetMajorVersion targetMajorVersion, boolean forced) {
        return new HandlerEvent<>(setupEvent(targetMajorVersion, forced));
    }

    private Event<UpgradeDatabaseServerRequest> setupEvent(TargetMajorVersion targetMajorVersion, boolean forced) {
        UpgradeDatabaseServerRequest request = new UpgradeDatabaseServerRequest(SDX_ID, "userId", targetMajorVersion, forced);
        return new Event<>(request);
    }

}

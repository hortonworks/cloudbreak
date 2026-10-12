package com.sequenceiq.datalake.flow.create.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.create.event.RdsTriggerRequest;
import com.sequenceiq.datalake.flow.create.event.RdsTriggeredEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
class RdsTriggerHandlerTest {

    private static final long SDX_ID = 12L;

    private static final String USER_ID = "userId";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private DatabaseService databaseService;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxStatusService sdxStatusService;

    private RdsCreationHandlerSupport rdsCreationHandlerSupport;

    @InjectMocks
    private RdsTriggerHandler underTest;

    private DetailedEnvironmentResponse environment;

    @BeforeEach
    void setUp() {
        rdsCreationHandlerSupport = new RdsCreationHandlerSupport();
        ReflectionTestUtils.setField(rdsCreationHandlerSupport, "durationInMinutes", 80);
        ReflectionTestUtils.setField(rdsCreationHandlerSupport, "sdxStatusService", sdxStatusService);
        ReflectionTestUtils.setField(underTest, "rdsCreationHandlerSupport", rdsCreationHandlerSupport);
        environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AZURE");
    }

    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo("RDSTRIGGERREQUEST");
    }

    @Test
    void defaultFailureEventTest() {
        IllegalStateException exception = new IllegalStateException("Bang!");

        Selectable result = underTest.defaultFailureEvent(SDX_ID, exception, new Event<>(request()));

        verifyFailed(result, null, exception);
    }

    @Test
    void firesNonBlockingCreateWhenParallelEnabled() {
        setParallelEnabled(true);
        mockSdx(true);

        assertThat(doAccept()).isInstanceOf(RdsTriggeredEvent.class);
        verify(databaseService).fireCreate(any(SdxCluster.class), eq(environment));
        verify(databaseService, never()).createAndWaitUntilAvailable(any(), any());
        verifyNoInteractions(sdxStatusService);
    }

    @Test
    void pollsCreateAndSetsStatusWhenParallelDisabled() {
        setParallelEnabled(false);
        SdxCluster sdx = mockSdx(true);

        assertThat(doAccept()).isInstanceOf(RdsTriggeredEvent.class);
        verify(databaseService).createAndWaitUntilAvailable(sdx, environment);
        verify(databaseService, never()).fireCreate(any(), any());
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.EXTERNAL_DATABASE_CREATED,
                "Data Lake external database created", sdx);
    }

    @Test
    void skipsDatabaseCreationWhenNoExternalDatabase() {
        setParallelEnabled(true);
        mockSdx(false);

        assertThat(doAccept()).isInstanceOf(RdsTriggeredEvent.class);
        verifyNoInteractions(databaseService);
        verifyNoInteractions(sdxStatusService);
    }

    @Test
    void failsWhenSdxNotFound() {
        when(environmentService.waitNetworkAndGetEnvironment(SDX_ID)).thenReturn(environment);
        when(sdxClusterRepository.findById(SDX_ID)).thenReturn(Optional.empty());

        Selectable result = doAccept();

        assertThat(result).isInstanceOf(SdxCreateFailedEvent.class);
        assertThat(((SdxEvent) result).getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void wrapsPollerStoppedExceptionWithTimeoutMessage() {
        when(environmentService.waitNetworkAndGetEnvironment(SDX_ID)).thenThrow(new PollerStoppedException("timed out"));

        Selectable result = doAccept();

        assertThat(result).isInstanceOf(SdxCreateFailedEvent.class);
        assertThat(result.getException())
                .isInstanceOf(PollerStoppedException.class)
                .hasMessage("Database creation timed out after 80 minutes");
    }

    @ParameterizedTest
    @MethodSource("exceptions")
    void propagatesExceptionAsFailedEvent(Exception exception) {
        when(environmentService.waitNetworkAndGetEnvironment(SDX_ID)).thenThrow(exception);

        verifyFailed(doAccept(), USER_ID, exception);
    }

    private static Object[] exceptions() {
        return new Object[] {
                new UserBreakException("userbreak"),
                new PollerException("pollerexception"),
                new IllegalStateException("runtime")
        };
    }

    private SdxCluster mockSdx(boolean externalDatabase) {
        SdxCluster sdx = new SdxCluster();
        sdx.setId(SDX_ID);
        SdxDatabase database = new SdxDatabase();
        database.setDatabaseAvailabilityType(externalDatabase
                ? SdxDatabaseAvailabilityType.HA
                : SdxDatabaseAvailabilityType.NONE);
        sdx.setSdxDatabase(database);
        when(environmentService.waitNetworkAndGetEnvironment(SDX_ID)).thenReturn(environment);
        when(sdxClusterRepository.findById(SDX_ID)).thenReturn(Optional.of(sdx));
        return sdx;
    }

    private void setParallelEnabled(boolean enabled) {
        ReflectionTestUtils.setField(underTest, "parallelRdsCreateEnabled", enabled);
    }

    private RdsTriggerRequest request() {
        return new RdsTriggerRequest(SDX_ID, USER_ID);
    }

    private Selectable doAccept() {
        return underTest.doAccept(new HandlerEvent<>(new Event<>(request())));
    }

    private void verifyFailed(Selectable result, String expectedUserId, Exception expectedException) {
        assertThat(result).isInstanceOf(SdxCreateFailedEvent.class);
        SdxCreateFailedEvent failed = (SdxCreateFailedEvent) result;
        assertThat(failed.getResourceId()).isEqualTo(SDX_ID);
        assertThat(failed.getUserId()).isEqualTo(expectedUserId);
        assertThat(failed.getException()).isSameAs(expectedException);
    }
}

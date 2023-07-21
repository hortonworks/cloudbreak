package com.sequenceiq.datalake.flow.delete.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.flow.delete.event.RdsDeletionSuccessEvent;
import com.sequenceiq.datalake.flow.delete.event.RdsDeletionWaitRequest;
import com.sequenceiq.datalake.flow.delete.event.SdxDeletionFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
class RdsDeletionHandlerTest {

    private static final int DURATION_IN_MINUTES = 15;

    private static final long DATALAKE_ID = 12L;

    private static final String USER_ID = "userId";

    private static final String DATALAKE_CRN = "datalakeCrn";

    private static final String REQUEST_ID = "requestId";

    private static final String DATABASE_CRN = "databaseCrn";

    private static final String EMPTY_DATABASE_CRN = "";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private DatabaseService databaseService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @InjectMocks
    private RdsDeletionHandler underTest;

    @Mock
    private Event<RdsDeletionWaitRequest> event;

    @Mock
    private HandlerEvent<RdsDeletionWaitRequest> handlerEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "durationInMinutes", DURATION_IN_MINUTES);

        MDCBuilder.addRequestId(REQUEST_ID);
    }

    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo("RdsDeletionWaitRequest");
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void defaultFailureEventTest(boolean forceDelete) {
        UnsupportedOperationException exception = new UnsupportedOperationException("Bang!");
        when(event.getData()).thenReturn(new RdsDeletionWaitRequest(DATALAKE_ID, USER_ID, forceDelete));

        Selectable result = underTest.defaultFailureEvent(DATALAKE_ID, exception, event);

        verifyFailedEvent(result, null, exception, forceDelete);
    }

    private void verifyFailedEvent(Selectable result, String userIdExpected, Exception exceptionExpected, boolean forceDeleteExpected) {
        verifyFailedEventInternal(result, userIdExpected, exceptionExpected, null, null, forceDeleteExpected);
    }

    private <E extends Exception> void verifyFailedEvent(Selectable result, String userIdExpected, Class<E> exceptionClassExpected,
            String exceptionMessageExpected, boolean forceDeleteExpected) {
        verifyFailedEventInternal(result, userIdExpected, null, exceptionClassExpected, exceptionMessageExpected, forceDeleteExpected);
    }

    private <E extends Exception> void verifyFailedEventInternal(Selectable result, String userIdExpected, Exception exceptionExpected,
            Class<E> exceptionClassExpected, String exceptionMessageExpected, boolean forceDeleteExpected) {
        assertThat(result).isInstanceOf(SdxDeletionFailedEvent.class);

        SdxDeletionFailedEvent sdxDeletionFailedEvent = (SdxDeletionFailedEvent) result;
        assertThat(sdxDeletionFailedEvent.getResourceId()).isEqualTo(DATALAKE_ID);
        assertThat(sdxDeletionFailedEvent.getUserId()).isEqualTo(userIdExpected);
        if (exceptionExpected != null) {
            assertThat(sdxDeletionFailedEvent.getException()).isSameAs(exceptionExpected);
        } else {
            assertThat(sdxDeletionFailedEvent.getException()).isInstanceOf(exceptionClassExpected);
            assertThat(sdxDeletionFailedEvent.getException()).hasMessage(exceptionMessageExpected);
        }
        assertThat(sdxDeletionFailedEvent.getSdxName()).isNull();
        assertThat(sdxDeletionFailedEvent.isForced()).isEqualTo(forceDeleteExpected);
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void doAcceptTestErrorWhenUserBreakException(boolean forceDelete) {
        initHandlerEvent(forceDelete);
        UserBreakException userBreakException = new UserBreakException("Problem");
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenThrow(userBreakException);

        Selectable result = underTest.doAccept(handlerEvent);

        verifyFailedEvent(result, USER_ID, userBreakException, forceDelete);
        verify(databaseService, never()).terminate(any(SdxCluster.class), anyBoolean());
        verify(sdxStatusService, never()).setStatusForDatalakeAndNotify(any(DatalakeStatusEnum.class), anyString(), any(SdxCluster.class));
        verify(ownerAssignmentService, never()).notifyResourceDeleted(anyString());
    }

    private void initHandlerEvent(boolean forceDelete) {
        when(handlerEvent.getData()).thenReturn(new RdsDeletionWaitRequest(DATALAKE_ID, USER_ID, forceDelete));
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void doAcceptTestErrorWhenPollerStoppedException(boolean forceDelete) {
        initHandlerEvent(forceDelete);
        PollerStoppedException pollerStoppedException = new PollerStoppedException("Problem");
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenThrow(pollerStoppedException);

        Selectable result = underTest.doAccept(handlerEvent);

        verifyFailedEvent(result, USER_ID, PollerStoppedException.class, "Database deletion timed out after 15 minutes", forceDelete);
        verify(databaseService, never()).terminate(any(SdxCluster.class), anyBoolean());
        verify(sdxStatusService, never()).setStatusForDatalakeAndNotify(any(DatalakeStatusEnum.class), anyString(), any(SdxCluster.class));
        verify(ownerAssignmentService, never()).notifyResourceDeleted(anyString());
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void doAcceptTestErrorWhenPollerException(boolean forceDelete) {
        initHandlerEvent(forceDelete);
        PollerException pollerException = new PollerException("Problem");
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenThrow(pollerException);

        Selectable result = underTest.doAccept(handlerEvent);

        verifyFailedEvent(result, USER_ID, pollerException, forceDelete);
        verify(databaseService, never()).terminate(any(SdxCluster.class), anyBoolean());
        verify(sdxStatusService, never()).setStatusForDatalakeAndNotify(any(DatalakeStatusEnum.class), anyString(), any(SdxCluster.class));
        verify(ownerAssignmentService, never()).notifyResourceDeleted(anyString());
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void doAcceptTestErrorWhenOtherException(boolean forceDelete) {
        initHandlerEvent(forceDelete);
        IllegalStateException exception = new IllegalStateException("Problem");
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenThrow(exception);

        Selectable result = underTest.doAccept(handlerEvent);

        verifyFailedEvent(result, USER_ID, exception, forceDelete);
        verify(databaseService, never()).terminate(any(SdxCluster.class), anyBoolean());
        verify(sdxStatusService, never()).setStatusForDatalakeAndNotify(any(DatalakeStatusEnum.class), anyString(), any(SdxCluster.class));
        verify(ownerAssignmentService, never()).notifyResourceDeleted(anyString());
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void doAcceptTestSkipTerminateWhenSdxClusterAbsent(boolean forceDelete) {
        initHandlerEvent(forceDelete);
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenReturn(Optional.empty());

        Selectable result = underTest.doAccept(handlerEvent);

        verifySuccessEvent(result);
        verify(databaseService, never()).terminate(any(SdxCluster.class), anyBoolean());
        verify(sdxStatusService, never()).setStatusForDatalakeAndNotify(any(DatalakeStatusEnum.class), anyString(), any(SdxCluster.class));
        verify(ownerAssignmentService, never()).notifyResourceDeleted(anyString());
    }

    private void verifySuccessEvent(Selectable result) {
        assertThat(result).isInstanceOf(RdsDeletionSuccessEvent.class);

        RdsDeletionSuccessEvent rdsDeletionSuccessEvent = (RdsDeletionSuccessEvent) result;
        assertThat(rdsDeletionSuccessEvent.getResourceId()).isEqualTo(DATALAKE_ID);
        assertThat(rdsDeletionSuccessEvent.getUserId()).isEqualTo(USER_ID);
        assertThat(rdsDeletionSuccessEvent.getSdxName()).isNull();
    }

    static Object[][] skipTerminateDataProvider() {
        return new Object[][]{
                // forceDelete sdxDatabaseAvailabilityType createDatabase databaseCrn
                {false, SdxDatabaseAvailabilityType.NON_HA, false, null},
                {false, SdxDatabaseAvailabilityType.NON_HA, true, null},
                {true, SdxDatabaseAvailabilityType.NON_HA, false, null},
                {true, SdxDatabaseAvailabilityType.NON_HA, true, null},
                {false, SdxDatabaseAvailabilityType.HA, false, null},
                {false, SdxDatabaseAvailabilityType.HA, true, null},
                {true, SdxDatabaseAvailabilityType.HA, false, null},
                {true, SdxDatabaseAvailabilityType.HA, true, null},
                {false, null, true, null},
                {true, null, true, null},
                {false, SdxDatabaseAvailabilityType.NON_HA, false, EMPTY_DATABASE_CRN},
                {false, SdxDatabaseAvailabilityType.NON_HA, true, EMPTY_DATABASE_CRN},
                {true, SdxDatabaseAvailabilityType.NON_HA, false, EMPTY_DATABASE_CRN},
                {true, SdxDatabaseAvailabilityType.NON_HA, true, EMPTY_DATABASE_CRN},
                {false, SdxDatabaseAvailabilityType.HA, false, EMPTY_DATABASE_CRN},
                {false, SdxDatabaseAvailabilityType.HA, true, EMPTY_DATABASE_CRN},
                {true, SdxDatabaseAvailabilityType.HA, false, EMPTY_DATABASE_CRN},
                {true, SdxDatabaseAvailabilityType.HA, true, EMPTY_DATABASE_CRN},
                {false, null, true, EMPTY_DATABASE_CRN},
                {true, null, true, EMPTY_DATABASE_CRN},
                {false, SdxDatabaseAvailabilityType.NONE, false, null},
                {false, SdxDatabaseAvailabilityType.NONE, true, null},
                {true, SdxDatabaseAvailabilityType.NONE, false, null},
                {true, SdxDatabaseAvailabilityType.NONE, true, null},
                {false, SdxDatabaseAvailabilityType.NONE, false, EMPTY_DATABASE_CRN},
                {false, SdxDatabaseAvailabilityType.NONE, true, EMPTY_DATABASE_CRN},
                {true, SdxDatabaseAvailabilityType.NONE, false, EMPTY_DATABASE_CRN},
                {true, SdxDatabaseAvailabilityType.NONE, true, EMPTY_DATABASE_CRN},
                {false, SdxDatabaseAvailabilityType.NONE, false, DATABASE_CRN},
                {false, SdxDatabaseAvailabilityType.NONE, true, DATABASE_CRN},
                {true, SdxDatabaseAvailabilityType.NONE, false, DATABASE_CRN},
                {true, SdxDatabaseAvailabilityType.NONE, true, DATABASE_CRN},
                {false, null, false, null},
                {true, null, false, null},
                {false, null, false, EMPTY_DATABASE_CRN},
                {true, null, false, EMPTY_DATABASE_CRN},
                {false, null, false, DATABASE_CRN},
                {true, null, false, DATABASE_CRN},
        };
    }

    @ParameterizedTest(name = "forceDelete={0}, sdxDatabaseAvailabilityType={1}, createDatabase={2}, databaseCrn={3}")
    @MethodSource("skipTerminateDataProvider")
    void doAcceptTestSkipTerminate(boolean forceDelete, SdxDatabaseAvailabilityType sdxDatabaseAvailabilityType, boolean createDatabase, String databaseCrn) {
        initHandlerEvent(forceDelete);
        SdxCluster sdxCluster = sdxCluster(sdxDatabaseAvailabilityType, createDatabase, databaseCrn);
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenReturn(Optional.of(sdxCluster));

        Selectable result = underTest.doAccept(handlerEvent);

        verifySuccessEvent(result);
        verify(databaseService, never()).terminate(any(SdxCluster.class), anyBoolean());
        verifyDeletedStatus(sdxCluster);
    }

    private SdxCluster sdxCluster(SdxDatabaseAvailabilityType databaseAvailabilityType, boolean createDatabase, String databaseCrn) {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setCrn(DATALAKE_CRN);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseAvailabilityType(databaseAvailabilityType);
        sdxDatabase.setCreateDatabase(createDatabase);
        sdxDatabase.setDatabaseCrn(databaseCrn);
        sdxCluster.setSdxDatabase(sdxDatabase);
        return sdxCluster;
    }

    private void verifyDeletedStatus(SdxCluster sdxCluster) {
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETED, "Datalake External RDS deleted", sdxCluster);
        verify(ownerAssignmentService).notifyResourceDeleted(DATALAKE_CRN);
    }

    static Object[][] executeTerminateDataProvider() {
        return new Object[][]{
                // forceDelete sdxDatabaseAvailabilityType createDatabase
                {false, SdxDatabaseAvailabilityType.NON_HA, false},
                {false, SdxDatabaseAvailabilityType.NON_HA, true},
                {true, SdxDatabaseAvailabilityType.NON_HA, false},
                {true, SdxDatabaseAvailabilityType.NON_HA, true},
                {false, SdxDatabaseAvailabilityType.HA, false},
                {false, SdxDatabaseAvailabilityType.HA, true},
                {true, SdxDatabaseAvailabilityType.HA, false},
                {true, SdxDatabaseAvailabilityType.HA, true},
                {false, null, true},
                {true, null, true},
        };
    }

    @ParameterizedTest(name = "forceDelete={0}, sdxDatabaseAvailabilityType={1}, createDatabase={2}")
    @MethodSource("executeTerminateDataProvider")
    void doAcceptTestExecuteTerminate(boolean forceDelete, SdxDatabaseAvailabilityType sdxDatabaseAvailabilityType, boolean createDatabase) {
        initHandlerEvent(forceDelete);
        SdxCluster sdxCluster = sdxCluster(sdxDatabaseAvailabilityType, createDatabase);
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenReturn(Optional.of(sdxCluster));

        Selectable result = underTest.doAccept(handlerEvent);

        verifySuccessEvent(result);
        verifyTerminateExecutedAndDeletedStatus(forceDelete, sdxCluster);
    }

    private SdxCluster sdxCluster(SdxDatabaseAvailabilityType sdxDatabaseAvailabilityType, boolean createDatabase) {
        return sdxCluster(sdxDatabaseAvailabilityType, createDatabase, DATABASE_CRN);
    }

    private void verifyTerminateExecutedAndDeletedStatus(boolean forceDelete, SdxCluster sdxCluster) {
        verify(databaseService).terminate(sdxCluster, forceDelete);
        verifyDeletedStatus(sdxCluster);
    }

    @ParameterizedTest(name = "forceDelete={0}, sdxDatabaseAvailabilityType={1}, createDatabase={2}")
    @MethodSource("executeTerminateDataProvider")
    void doAcceptTestExecuteTerminateAndNotFoundExceptionWhenSetStatusForDatalakeAndNotify(boolean forceDelete,
            SdxDatabaseAvailabilityType sdxDatabaseAvailabilityType, boolean createDatabase) {
        initHandlerEvent(forceDelete);
        SdxCluster sdxCluster = sdxCluster(sdxDatabaseAvailabilityType, createDatabase);
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenReturn(Optional.of(sdxCluster));
        doThrow(new NotFoundException("Data Lake not found"))
                .when(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETED, "Datalake External RDS deleted", sdxCluster);

        Selectable result = underTest.doAccept(handlerEvent);

        verifySuccessEvent(result);
        verifyTerminateExecutedAndDeletedStatus(forceDelete, sdxCluster);
    }

}
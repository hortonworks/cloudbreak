package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.WaitForExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.WaitForExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.WaitForExternalDatabaseSuccess;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class WaitForExternalDatabaseHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String DB_CRN = "crn:cdp:redbeams:us-west-1:acc:databaseServer:db";

    @Mock
    private ExternalDatabaseService externalDatabaseService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterView clusterView;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private StackView stackView;

    @InjectMocks
    private WaitForExternalDatabaseHandler underTest;

    @Test
    void selectorMatchesRequest() {
        assertThat(underTest.selector()).isEqualTo("WAITFOREXTERNALDATABASEREQUEST");
    }

    @Test
    void defaultFailureEventWrapsException() {
        Exception ex = new IllegalStateException("boom");

        Selectable result = underTest.defaultFailureEvent(STACK_ID, ex, new Event<>(new WaitForExternalDatabaseRequest(STACK_ID)));

        assertThat(result).isInstanceOf(WaitForExternalDatabaseFailed.class);
        assertThat(((WaitForExternalDatabaseFailed) result).getException()).isEqualTo(ex);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void skipsBarrierWhenNoExternalDatabase(String crn) {
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(clusterView);
        when(clusterView.getDatabaseServerCrn()).thenReturn(crn);

        assertThat(doAccept()).isInstanceOf(WaitForExternalDatabaseSuccess.class);
        verifyNoInteractions(externalDatabaseService);
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void skipsBarrierWhenClusterViewMissing() {
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(null);

        assertThat(doAccept()).isInstanceOf(WaitForExternalDatabaseSuccess.class);
        verifyNoInteractions(externalDatabaseService);
    }

    @Test
    void succeedsWhenExternalDatabaseServiceConfirmsAvailability() {
        mockCrn();

        assertThat(doAccept()).isInstanceOf(WaitForExternalDatabaseSuccess.class);
        verify(externalDatabaseService).waitForDatabaseAvailable(eq(clusterView), eq(DB_CRN), eq(true));
    }

    @ParameterizedTest
    @MethodSource("pollingExceptions")
    void failsWithWrappedExceptionWhenPollingDoesNotSucceed(Exception thrown) {
        mockCrn();
        doThrow(thrown).when(externalDatabaseService).waitForDatabaseAvailable(any(), any(), anyBoolean());

        Selectable result = doAccept();

        assertThat(result).isInstanceOf(WaitForExternalDatabaseFailed.class);
        assertThat(result.getException()).isSameAs(thrown);
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void failsWhenPollingTimesOut() {
        mockCrn();
        PollerStoppedException timeout = new PollerStoppedException("timed out after 1 min");
        doThrow(timeout).when(externalDatabaseService).waitForDatabaseAvailable(any(), any(), anyBoolean());

        Selectable result = doAccept();

        assertThat(result).isInstanceOf(WaitForExternalDatabaseFailed.class);
        assertThat(((WaitForExternalDatabaseFailed) result).getException()).isSameAs(timeout);
    }

    @Test
    void firesExternalDatabaseCreationFinishedEventForDatalakeKeepingTheStackStatus() {
        mockCrn();
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        when(stackView.isDatalake()).thenReturn(true);
        when(stackView.getStatus()).thenReturn(Status.CREATE_IN_PROGRESS);

        assertThat(doAccept()).isInstanceOf(WaitForExternalDatabaseSuccess.class);

        verify(cloudbreakEventService).fireCloudbreakEvent(STACK_ID, Status.CREATE_IN_PROGRESS.name(),
                ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_FINISHED);
    }

    @Test
    void doesNotFireTheEventForDatahubBecauseTheCreationFlowAlreadyDidIt() {
        mockCrn();
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        when(stackView.isDatalake()).thenReturn(false);

        assertThat(doAccept()).isInstanceOf(WaitForExternalDatabaseSuccess.class);

        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void stillSucceedsWhenSendingTheEventFails() {
        mockCrn();
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        when(stackView.isDatalake()).thenReturn(true);
        when(stackView.getStatus()).thenReturn(Status.CREATE_IN_PROGRESS);
        doThrow(new IllegalStateException("event store down")).when(cloudbreakEventService)
                .fireCloudbreakEvent(STACK_ID, Status.CREATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_FINISHED);

        assertThat(doAccept()).isInstanceOf(WaitForExternalDatabaseSuccess.class);
    }

    @Test
    void stillSucceedsWhenTheStackCannotBeLoadedForTheEvent() {
        mockCrn();
        when(stackDtoService.getStackViewById(STACK_ID)).thenThrow(new IllegalStateException("stack gone"));

        assertThat(doAccept()).isInstanceOf(WaitForExternalDatabaseSuccess.class);
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void failsWithTheServiceMessageWhenTheDatabaseIsGone() {
        mockCrn();
        CloudbreakServiceException gone = new CloudbreakServiceException("External database dbCrn was not found, it might have been deleted");
        doThrow(gone).when(externalDatabaseService).waitForDatabaseAvailable(any(), any(), anyBoolean());

        Selectable result = doAccept();

        assertThat(result).isInstanceOf(WaitForExternalDatabaseFailed.class);
        assertThat(result.getException()).isSameAs(gone);
        verifyNoInteractions(cloudbreakEventService);
    }

    private static Object[] pollingExceptions() {
        return new Object[] {
                new UserBreakException("user break"),
                new PollerStoppedException("stopped"),
                new PollerException("poller exception"),
                new IllegalStateException("unexpected")
        };
    }

    private void mockCrn() {
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(clusterView);
        when(clusterView.getDatabaseServerCrn()).thenReturn(DB_CRN);
    }

    private Selectable doAccept() {
        return underTest.doAccept(new HandlerEvent<>(new Event<>(new WaitForExternalDatabaseRequest(STACK_ID))));
    }
}

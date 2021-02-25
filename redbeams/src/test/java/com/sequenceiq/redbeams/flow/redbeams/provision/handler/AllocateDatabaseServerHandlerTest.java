package com.sequenceiq.redbeams.flow.redbeams.provision.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandlerTestSupport;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.sslcertificate.DatabaseServerSslCertificatePrescriptionService;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class AllocateDatabaseServerHandlerTest {

    private static final Long RESOURCE_ID = 1234L;

    private static final String STATUS_REASON_ERROR = "myerror";

    private static final String STATUS_REASON_SUCCESS = "all good";

    private static final long PRIVATE_ID_1 = 78L;

    private static final long PRIVATE_ID_2 = 56L;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private PollTaskFactory statusCheckFactory;

    @Mock
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Mock
    private DatabaseServerSslCertificatePrescriptionService databaseServerSslCertificatePrescriptionService;

    @InjectMocks
    private AllocateDatabaseServerHandler underTest;

    @Mock
    private Event<AllocateDatabaseServerRequest> event;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudPlatformVariant cloudPlatformVariant;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudConnector<Object> cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector<Object> resourceConnector;

    @Mock
    private PollTask<ResourcesStatePollerResult> task;

    @Mock
    private ResourcesStatePollerResult statePollerResult;

    private DBStack dbStack;

    private DatabaseStack databaseStack;

    @BeforeEach
    void setUp() {
        dbStack = new DBStack();
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        databaseStack = new DatabaseStack(null, null, Map.of(), "");
    }

    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo("ALLOCATEDATABASESERVERREQUEST");
    }

    @Test
    void defaultFailureEventTest() {
        Exception e = new Exception();

        Selectable selectable = underTest.defaultFailureEvent(RESOURCE_ID, e, event);

        verifyFailureEvent(e, selectable);
    }

    private void verifyFailureEvent(Exception e, Selectable selectable) {
        verifyFailureEvent(selectable);
        assertThat(extractException(selectable)).isSameAs(e);
    }

    private void verifyFailureEvent(Selectable selectable) {
        assertThat(selectable).isInstanceOf(AllocateDatabaseServerFailed.class);

        AllocateDatabaseServerFailed allocateDatabaseServerFailed = (AllocateDatabaseServerFailed) selectable;
        assertThat(allocateDatabaseServerFailed.getResourceId()).isEqualTo(RESOURCE_ID);
    }

    private Exception extractException(Selectable selectable) {
        return ((AllocateDatabaseServerFailed) selectable).getException();
    }

    @Test
    void doAcceptTestWhenFailureLaunchError() throws Exception {
        initCommon();

        Exception e = new Exception();
        when(resourceConnector.launchDatabaseServer(eq(authenticatedContext), any(DatabaseStack.class), eq(persistenceNotifier))).thenThrow(e);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(e, selectable);
    }

    @Test
    void doAcceptTestWhenFailurePollerError() throws Exception {
        initCommon();

        when(statusCheckFactory.newPollResourcesStateTask(eq(authenticatedContext), anyList(), eq(true))).thenReturn(task);
        when(task.completed(any(ResourcesStatePollerResult.class))).thenReturn(false);
        Exception e = new Exception();
        when(syncPollingScheduler.schedule(task)).thenThrow(e);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(e, selectable);
    }

    @Test
    void doAcceptTestWhenFailureNullPollerResult() throws Exception {
        initCommon();

        when(statusCheckFactory.newPollResourcesStateTask(eq(authenticatedContext), anyList(), eq(true))).thenReturn(task);
        when(task.completed(any(ResourcesStatePollerResult.class))).thenReturn(false);
        when(syncPollingScheduler.schedule(task)).thenReturn(null);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(selectable);
        Exception e = extractException(selectable);
        assertThat(e).isInstanceOf(IllegalStateException.class);
        assertThat(e).hasMessageStartingWith("ResourcesStatePollerResult is null, cannot check launch status of database stack for ");
    }

    @Test
    void doAcceptTestWhenFailureNullPollerResultResults() throws Exception {
        initCommon();

        when(statusCheckFactory.newPollResourcesStateTask(eq(authenticatedContext), anyList(), eq(true))).thenReturn(task);
        when(task.completed(any(ResourcesStatePollerResult.class))).thenReturn(false);
        when(syncPollingScheduler.schedule(task)).thenReturn(statePollerResult);
        when(statePollerResult.getResults()).thenReturn(null);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(selectable);
        Exception e = extractException(selectable);
        assertThat(e).isInstanceOf(IllegalStateException.class);
        assertThat(e).hasMessageStartingWith("ResourcesStatePollerResult.results is null, cannot check launch status of database stack for ");
    }

    static Object[][] doAcceptTestWhenFailureBadPollerResultDataProvider() {
        return new Object[][]{
                // testCaseName resourceStatus
                {"ResourceStatus.DELETED", ResourceStatus.DELETED},
                {"ResourceStatus.FAILED", ResourceStatus.FAILED},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("doAcceptTestWhenFailureBadPollerResultDataProvider")
    void doAcceptTestWhenFailureBadPollerResultSingleResource(String testCaseName, ResourceStatus resourceStatus) throws Exception {
        initCommon();

        when(statusCheckFactory.newPollResourcesStateTask(eq(authenticatedContext), anyList(), eq(true))).thenReturn(task);
        when(task.completed(any(ResourcesStatePollerResult.class))).thenReturn(false);
        when(syncPollingScheduler.schedule(task)).thenReturn(statePollerResult);
        CloudResourceStatus cloudResourceStatus = new CloudResourceStatus(null, resourceStatus, STATUS_REASON_ERROR, PRIVATE_ID_1);
        when(statePollerResult.getResults()).thenReturn(List.of(cloudResourceStatus));

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(selectable);
        Exception e = extractException(selectable);
        assertThat(e).isInstanceOf(OperationException.class);
        assertThat(e).hasMessageStartingWith("Failed to launch the database stack for ");
        assertThat(e).hasMessageEndingWith(" due to: " + STATUS_REASON_ERROR);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("doAcceptTestWhenFailureBadPollerResultDataProvider")
    void doAcceptTestWhenFailureBadPollerResultMultipleResources(String testCaseName, ResourceStatus resourceStatus) throws Exception {
        initCommon();

        when(statusCheckFactory.newPollResourcesStateTask(eq(authenticatedContext), anyList(), eq(true))).thenReturn(task);
        when(task.completed(any(ResourcesStatePollerResult.class))).thenReturn(false);
        when(syncPollingScheduler.schedule(task)).thenReturn(statePollerResult);
        CloudResourceStatus cloudResourceStatusSuccess = new CloudResourceStatus(null, ResourceStatus.CREATED, STATUS_REASON_SUCCESS, PRIVATE_ID_2);
        CloudResourceStatus cloudResourceStatusError = new CloudResourceStatus(null, resourceStatus, STATUS_REASON_ERROR, PRIVATE_ID_1);
        when(statePollerResult.getResults()).thenReturn(List.of(cloudResourceStatusSuccess, cloudResourceStatusError));

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(selectable);
        Exception e = extractException(selectable);
        assertThat(e).isInstanceOf(OperationException.class);
        assertThat(e).hasMessageStartingWith("Failed to launch the database stack for ");
        assertThat(e).hasMessageEndingWith(String.format(" due to: [%s]", cloudResourceStatusError.toString()));
    }

    @Test
    void doAcceptTestWhenSuccessWithoutPolling() throws Exception {
        initCommon();

        when(statusCheckFactory.newPollResourcesStateTask(eq(authenticatedContext), anyList(), eq(true))).thenReturn(task);
        when(task.completed(any(ResourcesStatePollerResult.class))).thenReturn(true);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        assertThat(selectable).isInstanceOf(AllocateDatabaseServerSuccess.class);

        AllocateDatabaseServerSuccess allocateDatabaseServerSuccess = (AllocateDatabaseServerSuccess) selectable;
        assertThat(allocateDatabaseServerSuccess.getResourceId()).isEqualTo(RESOURCE_ID);

        verify(syncPollingScheduler, never()).schedule(task);

        verify(databaseServerSslCertificatePrescriptionService).prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);
    }

    @Test
    void doAcceptTestWhenSuccessWithPolling() throws Exception {
        initCommon();

        when(statusCheckFactory.newPollResourcesStateTask(eq(authenticatedContext), anyList(), eq(true))).thenReturn(task);
        when(task.completed(any(ResourcesStatePollerResult.class))).thenReturn(false);
        when(syncPollingScheduler.schedule(task)).thenReturn(statePollerResult);
        CloudResourceStatus cloudResourceStatus = new CloudResourceStatus(null, ResourceStatus.CREATED, STATUS_REASON_ERROR, PRIVATE_ID_1);
        when(statePollerResult.getResults()).thenReturn(List.of(cloudResourceStatus));

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        assertThat(selectable).isInstanceOf(AllocateDatabaseServerSuccess.class);

        AllocateDatabaseServerSuccess allocateDatabaseServerSuccess = (AllocateDatabaseServerSuccess) selectable;
        assertThat(allocateDatabaseServerSuccess.getResourceId()).isEqualTo(RESOURCE_ID);

        verify(syncPollingScheduler).schedule(task);

        verify(databaseServerSslCertificatePrescriptionService).prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);
    }

    private void initCommon() {
        when(cloudContext.getId()).thenReturn(RESOURCE_ID);
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);

        AllocateDatabaseServerRequest request = new AllocateDatabaseServerRequest(cloudContext, cloudCredential, dbStack, databaseStack);
        when(event.getData()).thenReturn(request);

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
    }

}
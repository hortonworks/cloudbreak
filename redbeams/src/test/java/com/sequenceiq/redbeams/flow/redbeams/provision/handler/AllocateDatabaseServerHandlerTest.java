package com.sequenceiq.redbeams.flow.redbeams.provision.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.service.CloudResourceValidationService;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandlerTestSupport;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4StackRequest;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.stack.Network;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.DatabaseCapabilityService;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.network.NetworkBuilderService;
import com.sequenceiq.redbeams.service.sslcertificate.DatabaseServerSslCertificatePrescriptionService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class AllocateDatabaseServerHandlerTest {

    private static final Long RESOURCE_ID = 1234L;

    private static final String STATUS_REASON_ERROR = "myerror";

    private static final String STATUS_REASON_SUCCESS = "all good";

    private static final long PRIVATE_ID_1 = 78L;

    private static final long PRIVATE_ID_2 = 56L;

    private static final String ENV_CRN = "envcrn";

    private static final Long NETWORK_ID = 765L;

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

    @Mock
    private DBStackService dbStackService;

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
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector resourceConnector;

    @Mock
    private PollTask<ResourcesStatePollerResult> task;

    @Mock
    private ResourcesStatePollerResult statePollerResult;

    @Mock
    private NetworkBuilderService networkBuilderService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private DBStackToDatabaseStackConverter dbStackToDatabaseStackConverter;

    @Mock
    private DatabaseCapabilityService databaseCapabilityService;

    @Mock
    private CloudResourceValidationService cloudResourceValidationService;

    private DBStack dbStack;

    private DatabaseStack databaseStack;

    @BeforeEach
    void setUp() {
        dbStack = new DBStack();
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        dbStack.setEnvironmentId(ENV_CRN);
        DatabaseServer databaseServer = new DatabaseServer();
        databaseServer.setInstanceType("type");
        dbStack.setDatabaseServer(databaseServer);
        lenient().when(dbStackService.getById(anyLong())).thenReturn(dbStack);
        databaseStack = new DatabaseStack(null, com.sequenceiq.cloudbreak.cloud.model.DatabaseServer.builder().build(), Map.of(), "");
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
    void doAcceptTestWhenFailureDuringValidation() throws Exception {
        initCommon();

        when(statusCheckFactory.newPollResourcesStateTask(eq(authenticatedContext), anyList(), eq(true))).thenReturn(task);
        when(task.completed(any(ResourcesStatePollerResult.class))).thenReturn(false);
        when(syncPollingScheduler.schedule(task)).thenReturn(statePollerResult);
        Exception e = new RuntimeException();
        doThrow(e).when(cloudResourceValidationService).validateResourcesState(cloudContext, statePollerResult);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(e, selectable);
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

        verify(databaseServerSslCertificatePrescriptionService).prescribeSslCertificateIfNeeded(cloudContext,
                cloudCredential,
                dbStack,
                databaseStack,
                Optional.empty());
        verify(dbStackService).save(dbStack);
        assertEquals(NETWORK_ID, dbStack.getNetwork());
    }

    @Test
    void doAcceptTestWhenSuccessWithPolling() throws Exception {
        initCommon();

        when(statusCheckFactory.newPollResourcesStateTask(eq(authenticatedContext), anyList(), eq(true))).thenReturn(task);
        when(task.completed(any(ResourcesStatePollerResult.class))).thenReturn(false);
        when(syncPollingScheduler.schedule(task)).thenReturn(statePollerResult);
        CloudResourceStatus cloudResourceStatus = new CloudResourceStatus(null, ResourceStatus.CREATED, STATUS_REASON_ERROR, PRIVATE_ID_1);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        assertThat(selectable).isInstanceOf(AllocateDatabaseServerSuccess.class);

        AllocateDatabaseServerSuccess allocateDatabaseServerSuccess = (AllocateDatabaseServerSuccess) selectable;
        assertThat(allocateDatabaseServerSuccess.getResourceId()).isEqualTo(RESOURCE_ID);

        verify(syncPollingScheduler).schedule(task);

        verify(databaseServerSslCertificatePrescriptionService)
                .prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack, Optional.empty());
        verify(dbStackService).save(dbStack);
        assertEquals(NETWORK_ID, dbStack.getNetwork());
    }

    @Test
    void doAcceptTestWhenInstanceTypeIsMissing() throws Exception {
        initCommon();
        dbStack.getDatabaseServer().setInstanceType(null);
        dbStack.setNetwork(1L);

        when(statusCheckFactory.newPollResourcesStateTask(eq(authenticatedContext), anyList(), eq(true))).thenReturn(task);
        when(task.completed(any(ResourcesStatePollerResult.class))).thenReturn(true);
        when(databaseCapabilityService.getDefaultInstanceType(any(CloudConnector.class), any(com.sequenceiq.cloudbreak.cloud.model.CloudCredential.class),
                any(CloudPlatformVariant.class), any(Region.class))).thenReturn("defaultInstanceType");

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        assertThat(selectable).isInstanceOf(AllocateDatabaseServerSuccess.class);

        AllocateDatabaseServerSuccess allocateDatabaseServerSuccess = (AllocateDatabaseServerSuccess) selectable;
        assertThat(allocateDatabaseServerSuccess.getResourceId()).isEqualTo(RESOURCE_ID);

        verify(databaseCapabilityService).getDefaultInstanceType(cloudConnector, cloudCredential, cloudPlatformVariant, Region.region(dbStack.getRegion()));
        verify(dbStackService).save(dbStack);
        assertEquals("defaultInstanceType", dbStack.getDatabaseServer().getInstanceType());
        ArgumentCaptor<DatabaseStack> databaseStackArgumentCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        verify(resourceConnector).launchDatabaseServer(any(AuthenticatedContext.class), databaseStackArgumentCaptor.capture(), any(PersistenceNotifier.class));
        assertEquals("defaultInstanceType", databaseStackArgumentCaptor.getValue().getDatabaseServer().getFlavor());
    }

    private void initCommon() {
        when(cloudContext.getId()).thenReturn(RESOURCE_ID);
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);

        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        lenient().when(environmentService.getByCrn(ENV_CRN)).thenReturn(environmentResponse);
        NetworkV4StackRequest networkParameters = new NetworkV4StackRequest();
        Network network = new Network();
        network.setId(NETWORK_ID);
        lenient().when(networkBuilderService.buildNetwork(networkParameters, environmentResponse, dbStack)).thenReturn(network);
        AllocateDatabaseServerRequest request = new AllocateDatabaseServerRequest(cloudContext, cloudCredential, databaseStack, networkParameters);
        when(event.getData()).thenReturn(request);

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        lenient().when(dbStackToDatabaseStackConverter.convert(dbStack)).thenReturn(databaseStack);
    }

}

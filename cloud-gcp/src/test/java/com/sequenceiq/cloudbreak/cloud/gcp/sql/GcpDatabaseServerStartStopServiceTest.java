package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.InstancesListResponse;
import com.google.api.services.sqladmin.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.poller.DatabasePollerService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpOperationUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

@ExtendWith(MockitoExtension.class)
class GcpDatabaseServerStartStopServiceTest {

    private static final String PROJECT_ID = "project-id";

    private static final String SERVER_NAME = "server-1";

    @Mock
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private DatabasePollerService databasePollerService;

    @Mock
    private GcpDatabaseOperationRetryService gcpDatabaseOperationRetryService;

    @InjectMocks
    private GcpDatabaseServerStopService stopService;

    @InjectMocks
    private GcpDatabaseServerStartService startService;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private SQLAdmin sqlAdmin;

    @Mock
    private SQLAdmin.Instances sqlAdminInstances;

    @Mock
    private SQLAdmin.Instances.List sqlAdminInstancesList;

    @Mock
    private SQLAdmin.Instances.Patch patch;

    @BeforeEach
    void setUp() throws Exception {
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudContext cloudContext = mock(CloudContext.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn(SERVER_NAME);
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn(PROJECT_ID);
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(PROJECT_ID)).thenReturn(sqlAdminInstancesList);
    }

    @Test
    void testStopSubmitsPatchThroughRetryServiceAndPollsOperation() throws Exception {
        instanceExists();
        when(sqlAdminInstances.patch(eq(PROJECT_ID), eq(SERVER_NAME), any(DatabaseInstance.class))).thenReturn(patch);
        when(gcpDatabaseOperationRetryService.executeRetryingOnConflict(patch, sqlAdmin, PROJECT_ID, SERVER_NAME))
                .thenReturn(new Operation().setName("operation-1"));

        stopService.stop(authenticatedContext, databaseStack);

        ArgumentCaptor<DatabaseInstance> patchedInstance = ArgumentCaptor.forClass(DatabaseInstance.class);
        verify(sqlAdminInstances).patch(eq(PROJECT_ID), eq(SERVER_NAME), patchedInstance.capture());
        assertEquals("NEVER", patchedInstance.getValue().getSettings().getActivationPolicy());
        ArgumentCaptor<List<CloudResource>> polledResources = ArgumentCaptor.forClass(List.class);
        verify(databasePollerService).startDatabasePoller(eq(authenticatedContext), polledResources.capture());
        assertEquals("operation-1", polledResources.getValue().getFirst().getStringParameter(GcpOperationUtil.OPERATION_ID));
    }

    @Test
    void testStartSubmitsPatchWithAlwaysPolicy() throws Exception {
        instanceExists();
        when(sqlAdminInstances.patch(eq(PROJECT_ID), eq(SERVER_NAME), any(DatabaseInstance.class))).thenReturn(patch);
        when(gcpDatabaseOperationRetryService.executeRetryingOnConflict(patch, sqlAdmin, PROJECT_ID, SERVER_NAME))
                .thenReturn(new Operation().setName("operation-1"));

        startService.start(authenticatedContext, databaseStack);

        ArgumentCaptor<DatabaseInstance> patchedInstance = ArgumentCaptor.forClass(DatabaseInstance.class);
        verify(sqlAdminInstances).patch(eq(PROJECT_ID), eq(SERVER_NAME), patchedInstance.capture());
        assertEquals("ALWAYS", patchedInstance.getValue().getSettings().getActivationPolicy());
        verify(databasePollerService).startDatabasePoller(eq(authenticatedContext), anyList());
    }

    @Test
    void testStopFailsWithStatusAndReasonWhenConflictPersists() throws Exception {
        instanceExists();
        GoogleJsonResponseException conflictException = conflictException();
        when(sqlAdminInstances.patch(eq(PROJECT_ID), eq(SERVER_NAME), any(DatabaseInstance.class))).thenReturn(patch);
        when(gcpDatabaseOperationRetryService.executeRetryingOnConflict(patch, sqlAdmin, PROJECT_ID, SERVER_NAME))
                .thenThrow(conflictException);

        GcpResourceException exception = assertThrows(GcpResourceException.class, () -> stopService.stop(authenticatedContext, databaseStack));

        assertEquals("Operation failed because another operation was already in progress. (HTTP 409, reason: operationInProgress): "
                + "[ resourceType: GCP_DATABASE,  resourceName: server-1 ]", exception.getMessage());
        assertSame(conflictException, exception.getCause());
        verify(databasePollerService, never()).startDatabasePoller(any(), anyList());
    }

    @Test
    void testStopDoesNothingWhenInstanceIsMissing() throws Exception {
        when(sqlAdminInstancesList.execute()).thenReturn(new InstancesListResponse());

        stopService.stop(authenticatedContext, databaseStack);

        verify(sqlAdminInstances, never()).patch(anyString(), anyString(), any(DatabaseInstance.class));
        verifyNoInteractions(gcpDatabaseOperationRetryService, databasePollerService);
    }

    private void instanceExists() throws Exception {
        when(sqlAdminInstancesList.execute()).thenReturn(new InstancesListResponse().setItems(List.of(new DatabaseInstance().setName(SERVER_NAME))));
    }

    private GoogleJsonResponseException conflictException() {
        GoogleJsonError.ErrorInfo errorInfo = new GoogleJsonError.ErrorInfo();
        errorInfo.setReason("operationInProgress");
        errorInfo.setMessage("Operation failed because another operation was already in progress.");
        GoogleJsonError details = new GoogleJsonError();
        details.setCode(409);
        details.setMessage("Operation failed because another operation was already in progress.");
        details.setErrors(List.of(errorInfo));
        return new GoogleJsonResponseException(new HttpResponseException.Builder(409, "Conflict", new HttpHeaders()), details);
    }
}

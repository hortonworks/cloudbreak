package com.sequenceiq.redbeams.sync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@ExtendWith(MockitoExtension.class)
public class DBStackStatusSyncServiceTest {

    private static final String ENVIRONMENT_ID = "environment id";

    private static final Long DB_STACK_ID = 1234L;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

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
    private Credential credential;

    @Mock
    private DBStack dbStack;

    @Mock
    private Crn crn;

    @Mock
    private DBStackJobService dbStackJobService;

    @Mock
    private DBStackToDatabaseStackConverter databaseStackConverter;

    private ArgumentCaptor<CloudContext> cloudContextArgumentCaptor;

    @InjectMocks
    private DBStackStatusSyncService victim;

    public static Stream<Arguments> provideTestData() {
        return Stream.of(
                //Status should not be updated as saved and current are the same
                Arguments.of(Status.AVAILABLE, ExternalDatabaseStatus.STARTED, null),
                Arguments.of(Status.STOPPED, ExternalDatabaseStatus.STOPPED, null),
                //ExternalDatabaseStatus should be converted to the correct DetailedDBStackStatus and update should be applied
                Arguments.of(Status.STOPPED, ExternalDatabaseStatus.STARTED, DetailedDBStackStatus.STARTED),
                Arguments.of(Status.AVAILABLE, ExternalDatabaseStatus.START_IN_PROGRESS, DetailedDBStackStatus.START_IN_PROGRESS),
                Arguments.of(Status.AVAILABLE, ExternalDatabaseStatus.STOPPED, DetailedDBStackStatus.STOPPED),
                Arguments.of(Status.AVAILABLE, ExternalDatabaseStatus.STOP_IN_PROGRESS, DetailedDBStackStatus.STOP_IN_PROGRESS),
                //UPDATE_IN_PROGRESS status covers all non handled statuses. In this case DB Stack status should not be updated.
                Arguments.of(Status.AVAILABLE, ExternalDatabaseStatus.UPDATE_IN_PROGRESS, DetailedDBStackStatus.UNKNOWN)
        );
    }

    @BeforeEach
    public void initTests() {
        MockitoAnnotations.initMocks(this);
        cloudContextArgumentCaptor = ArgumentCaptor.forClass(CloudContext.class);

        when(dbStack.getEnvironmentId()).thenReturn(ENVIRONMENT_ID);
        when(dbStack.getResourceCrn()).thenReturn(CrnTestUtil.getDatabaseServerCrnBuilder()
                .setAccountId("acc")
                .setResource("resource")
                .build().toString());
        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_ID)).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContextArgumentCaptor.capture(), Mockito.eq(cloudCredential))).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(databaseStackConverter.convert(dbStack)).thenReturn(databaseStack);
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    public void testStatusUpdate(Status savedStatus, ExternalDatabaseStatus externalDatabaseStatus, DetailedDBStackStatus newDetailedDBStackStatus)
            throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, databaseStack)).thenReturn(externalDatabaseStatus);
        when(dbStack.getId()).thenReturn(DB_STACK_ID);
        when(dbStack.getStatus()).thenReturn(savedStatus);
        when(dbStack.getOwnerCrn()).thenReturn(crn);

        victim.sync(dbStack);

        if (newDetailedDBStackStatus != null && newDetailedDBStackStatus.getStatus() != null) {
            verify(dbStackStatusUpdater).updateStatus(DB_STACK_ID, newDetailedDBStackStatus);
        } else {
            verifyZeroInteractions(dbStackStatusUpdater);
        }
        verifyZeroInteractions(dbStackJobService);
    }

    @Test
    public void shouldSetStatusAndUnscheduleInCaseOfStopCompleted()
            throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, databaseStack)).thenReturn(ExternalDatabaseStatus.DELETED);
        when(dbStack.getId()).thenReturn(DB_STACK_ID);
        when(dbStack.getStatus()).thenReturn(Status.DELETE_IN_PROGRESS);
        when(dbStack.getOwnerCrn()).thenReturn(crn);

        victim.sync(dbStack);

        verify(dbStackStatusUpdater).updateStatus(DB_STACK_ID, DetailedDBStackStatus.DELETE_COMPLETED);
        verify(dbStackJobService).unschedule(dbStack);
    }
}
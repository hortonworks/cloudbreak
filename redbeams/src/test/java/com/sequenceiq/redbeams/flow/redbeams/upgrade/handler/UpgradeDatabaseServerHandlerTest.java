package com.sequenceiq.redbeams.flow.redbeams.upgrade.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.UpgradeDatabaseServerRequest;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.stack.DBResourceService;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.upgrade.DBUpgradeMigrationService;

@ExtendWith(MockitoExtension.class)
public class UpgradeDatabaseServerHandlerTest {
    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:resourceCrn";

    @Mock
    private DBStackService dbStackService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudPlatformVariant cloudPlatformVariant;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector resourceConnector;

    @Mock
    private DBResourceService dbResourceService;

    @Mock
    private DBUpgradeMigrationService dbUpgradeMigrationService;

    @Mock
    private DatabaseServerConfigService databaseServerConfigService;

    @InjectMocks
    private UpgradeDatabaseServerHandler underTest;

    @Test
    void testSelector() {
        assertEquals("UPGRADEDATABASESERVERREQUEST", underTest.selector());
    }

    @Test
    void testDoAccept() {
        HandlerEvent<UpgradeDatabaseServerRequest> event = getHandlerEvent(false);
        DBStack dbStack = new DBStack();
        DatabaseServer databaseServer = new DatabaseServer();
        dbStack.setDatabaseServer(databaseServer);
        dbStack.setCloudPlatform(CloudPlatform.AZURE.name());

        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(dbStackService.getById(event.getData().getResourceId())).thenReturn(dbStack);
        ArgumentCaptor<DBStack> dbStackArgumentCaptor = ArgumentCaptor.forClass(DBStack.class);

        Selectable nextFlowStepSelector = underTest.doAccept(event);

        verify(dbStackService).getById(event.getData().getResourceId());
        verify(dbStackService).save(dbStackArgumentCaptor.capture());
        verify(dbUpgradeMigrationService, never()).mergeDatabaseStacks(eq(dbStack), any(), eq(cloudConnector), isNull(), isNull(), isNull());

        assertEquals("UPGRADEDATABASESERVERSUCCESS", nextFlowStepSelector.selector());
        assertEquals(event.getData().getTargetMajorVersion().getMajorVersion(), dbStackArgumentCaptor.getValue().getMajorVersion().getMajorVersion());
    }

    @ParameterizedTest
    @MethodSource("migratedUserNames")
    void testDoAcceptWithMigrationRequest(String originalUserName, String migratedUserName) {
        HandlerEvent<UpgradeDatabaseServerRequest> event = getHandlerEvent(true);
        DBStack dbStack = new DBStack();
        DatabaseServer databaseServer = new DatabaseServer();
        dbStack.setDatabaseServer(databaseServer);
        dbStack.setCloudPlatform(CloudPlatform.AZURE.name());
        dbStack.setResourceCrn(RESOURCE_CRN);
        DatabaseStack databaseStack = generateDatabaseStack();
        UpgradeDatabaseMigrationParams migrationParams = getMigrationParams();

        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(dbStackService.getById(event.getData().getResourceId())).thenReturn(dbStack);
        when(dbUpgradeMigrationService.mergeDatabaseStacks(any(DBStack.class), any(), any(), any(), any(), any())).thenReturn(databaseStack);
        DatabaseServerConfig databaseServerConfig = new DatabaseServerConfig();
        databaseServerConfig.setConnectionUserName(originalUserName);
        when(databaseServerConfigService.getByCrn(any(Crn.class))).thenReturn(Optional.of(databaseServerConfig));
        ArgumentCaptor<DBStack> dbStackArgumentCaptor = ArgumentCaptor.forClass(DBStack.class);
        ArgumentCaptor<DatabaseServerConfig> databaseServerConfigArgumentCaptor = ArgumentCaptor.forClass(DatabaseServerConfig.class);

        Selectable nextFlowStepSelector = underTest.doAccept(event);

        verify(dbStackService, times(2)).getById(event.getData().getResourceId());
        verify(dbStackService).save(dbStackArgumentCaptor.capture());
        verify(dbUpgradeMigrationService).mergeDatabaseStacks(any(DBStack.class), eq(migrationParams), eq(cloudConnector), eq(cloudCredential),
                eq(cloudPlatformVariant), isNull());
        verify(databaseServerConfigService).update(databaseServerConfigArgumentCaptor.capture());
        assertEquals("UPGRADEDATABASESERVERSUCCESS", nextFlowStepSelector.selector());
        DBStack actualDbStack = dbStackArgumentCaptor.getValue();
        DatabaseServer actualDbServer = actualDbStack.getDatabaseServer();
        assertEquals(migrationParams.getAttributes(), actualDbServer.getAttributes());
        assertEquals(migrationParams.getInstanceType(), actualDbServer.getInstanceType());
        assertEquals(migrationParams.getRootUserName(), actualDbServer.getRootUserName());
        assertEquals(migrationParams.getStorageSize(), actualDbServer.getStorageSize());
        assertNull(actualDbServer.getSecurityGroup());
        assertNull(actualDbServer.getDatabaseVendor());
        assertNull(actualDbServer.getAccountId());
        assertNull(actualDbServer.getConnectionDriver());
        assertNull(actualDbServer.getName());
        assertNull(actualDbServer.getDescription());
        assertNull(actualDbServer.getRootPassword());
        assertNull(actualDbServer.getPort());
        DatabaseServerConfig actualDatabaseServerConfig = databaseServerConfigArgumentCaptor.getValue();
        assertEquals(migratedUserName, actualDatabaseServerConfig.getConnectionUserName());
    }

    private static Stream<Arguments> migratedUserNames() {
        return Stream.of(
                Arguments.of("username@cuttable", "username"),
                Arguments.of("username", "username"));
    }

    @Test
    void testDefaultFailureEvent() {
        UpgradeDatabaseServerRequest upgradeDatabaseServerRequest = new UpgradeDatabaseServerRequest(null, null, null, null, null);

        Selectable defaultFailureEvent = underTest.defaultFailureEvent(1L, new RuntimeException(), Event.wrap(upgradeDatabaseServerRequest));

        assertEquals("REDBEAMSUPGRADEFAILEDEVENT", defaultFailureEvent.selector());
    }

    private HandlerEvent<UpgradeDatabaseServerRequest> getHandlerEvent(boolean includeMigrationParams) {
        UpgradeDatabaseServerRequest upgradeDatabaseServerRequest = new UpgradeDatabaseServerRequest(cloudContext, cloudCredential, null,
                TargetMajorVersion.VERSION_11, includeMigrationParams ? getMigrationParams() : null);
        HandlerEvent<UpgradeDatabaseServerRequest> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getEvent()).thenReturn(Event.wrap(upgradeDatabaseServerRequest));
        when(handlerEvent.getData()).thenReturn(upgradeDatabaseServerRequest);
        return handlerEvent;
    }

    private UpgradeDatabaseMigrationParams getMigrationParams() {
        UpgradeDatabaseMigrationParams migrationParams = new UpgradeDatabaseMigrationParams();
        migrationParams.setStorageSize(128L);
        migrationParams.setInstanceType("Standard_E4ds_v4");
        Map<String, Object> parameters = Map.of("key", "test");
        Json attributes = new Json(parameters);
        migrationParams.setAttributes(attributes);
        return migrationParams;
    }

    private DatabaseStack generateDatabaseStack() {
        UpgradeDatabaseMigrationParams migrationParams = getMigrationParams();
        com.sequenceiq.cloudbreak.cloud.model.DatabaseServer databaseServerModel = com.sequenceiq.cloudbreak.cloud.model.DatabaseServer.builder()
                .withServerId("dbname")
                .withFlavor(migrationParams.getInstanceType())
                .withStorageSize(migrationParams.getStorageSize())
                .withRootUserName("root")
                .withRootPassword("pwd")
                .withLocation("location")
                .withParams(migrationParams.getAttributes().getMap())
                .build();
        return new DatabaseStack(null, databaseServerModel, Map.of("tag1", "tag1"), "");
    }
}

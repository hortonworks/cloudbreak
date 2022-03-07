package com.sequenceiq.redbeams.flow.redbeams.provision.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandlerTestSupport;
import com.sequenceiq.redbeams.TestData;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationFailed;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationSuccess;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.sslcertificate.DatabaseServerSslCertificateSyncService;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class UpdateDatabaseServerRegistrationHandlerTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String DB_STACK_NAME = "dbStackName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String CONNECTION_DRIVER = "connectionDriver";

    private static final String ROOT_USER_NAME = "rootUserName";

    private static final String ROOT_PASSWORD = "rootPassword";

    private static final int PORT = 8642;

    private static final String DB_HOST_NAME = "dbHostName.database.azure.com";

    private static final String DB_SHORT_HOST_NAME = "dbHostName";

    private static final Crn CRN = TestData.getTestCrn("database", "name");

    private static final Long RESOURCE_ID = 1234L;

    @Mock
    private DatabaseServerConfigService databaseServerConfigService;

    @Mock
    private UserGeneratorService userGeneratorService;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @Mock
    private DatabaseServerSslCertificateSyncService databaseServerSslCertificateSyncService;

    @InjectMocks
    private UpdateDatabaseServerRegistrationHandler underTest;

    @Mock
    private Event<UpdateDatabaseServerRegistrationRequest> event;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Captor
    private ArgumentCaptor<DatabaseServerConfig> databaseServerConfigCaptor;

    private DatabaseStack databaseStack;

    @BeforeEach
    void setUp() {
        databaseStack = new DatabaseStack(null, null, Map.of(), "");
    }

    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo("UPDATEDATABASESERVERREGISTRATIONREQUEST");
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
        assertThat(selectable).isInstanceOf(UpdateDatabaseServerRegistrationFailed.class);

        UpdateDatabaseServerRegistrationFailed allocateDatabaseServerFailed = (UpdateDatabaseServerRegistrationFailed) selectable;
        assertThat(allocateDatabaseServerFailed.getResourceId()).isEqualTo(RESOURCE_ID);
    }

    private Exception extractException(Selectable selectable) {
        return ((UpdateDatabaseServerRegistrationFailed) selectable).getException();
    }

    @Test
    void doAcceptTestWhenFailureDatabaseServerConfigNotFound() throws Exception {
        DBStack dbStack = getDBStack();
        UpdateDatabaseServerRegistrationRequest request = getRequest(dbStack);
        when(event.getData()).thenReturn(request);

        when(databaseServerConfigService.getByCrn(CRN)).thenReturn(Optional.empty());

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(selectable);
        Exception e = extractException(selectable);
        assertThat(e).isInstanceOf(IllegalStateException.class);
        assertThat(e).hasMessage("Cannot find database server " + CRN);
    }

    @Test
    void doAcceptTestWhenFailureCloudResourceHostnameNotFound() throws Exception {
        DBStack dbStack = getDBStack();
        UpdateDatabaseServerRegistrationRequest request = getRequest(dbStack);
        when(event.getData()).thenReturn(request);

        when(cloudResourceHelper.getResourceTypeFromList(eq(ResourceType.RDS_HOSTNAME), any())).thenReturn(Optional.empty());

        when(databaseServerConfigService.getByCrn(CRN)).thenReturn(Optional.of(getOriginalDatabaseServerConfig()));

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(selectable);
        Exception e = extractException(selectable);
        assertThat(e).isInstanceOf(IllegalStateException.class);
        assertThat(e).hasMessage("DB hostname not found for allocated database.");
    }

    @Test
    void doAcceptTestWhenFailureCloudResourcePortNotFound() throws Exception {
        DBStack dbStack = getDBStack();
        UpdateDatabaseServerRegistrationRequest request = getRequest(dbStack);
        when(event.getData()).thenReturn(request);

        when(cloudResourceHelper.getResourceTypeFromList(eq(ResourceType.RDS_HOSTNAME), any())).thenReturn(Optional.of(mock(CloudResource.class)));
        when(cloudResourceHelper.getResourceTypeFromList(eq(ResourceType.RDS_PORT), any())).thenReturn(Optional.empty());

        DatabaseServerConfig originalDatabaseServerConfig = getOriginalDatabaseServerConfig();
        when(databaseServerConfigService.getByCrn(CRN)).thenReturn(Optional.of(originalDatabaseServerConfig));

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(selectable);
        Exception e = extractException(selectable);
        assertThat(e).isInstanceOf(IllegalStateException.class);
        assertThat(e).hasMessage("DB port not found for allocated database.");
    }

    @Test
    void doAcceptTestWhenSuccess() throws Exception {
        DBStack dbStack = getDBStack();
        dbStack.setCloudPlatform(CloudPlatform.AZURE.toString());
        addDatabaseServerToDBStack(dbStack);
        setupMocksMinimal(dbStack);

        when(userGeneratorService.updateUserName(ROOT_USER_NAME, Optional.of(CloudPlatform.AZURE), DB_HOST_NAME))
            .thenReturn(ROOT_USER_NAME + "@" + DB_SHORT_HOST_NAME);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        assertThat(selectable).isInstanceOf(UpdateDatabaseServerRegistrationSuccess.class);

        UpdateDatabaseServerRegistrationSuccess updateDatabaseServerRegistrationSuccess = (UpdateDatabaseServerRegistrationSuccess) selectable;
        assertThat(updateDatabaseServerRegistrationSuccess.getResourceId()).isEqualTo(RESOURCE_ID);

        verify(databaseServerConfigService).update(databaseServerConfigCaptor.capture());
        DatabaseServerConfig databaseServerConfig = databaseServerConfigCaptor.getValue();
        assertThat(databaseServerConfig.getHost()).isEqualTo(DB_HOST_NAME);
        assertThat(databaseServerConfig.getPort().intValue()).isEqualTo(PORT);

        assertThat(databaseServerConfig.getConnectionUserName()).isEqualTo(ROOT_USER_NAME + "@" + DB_SHORT_HOST_NAME);

        verify(databaseServerSslCertificateSyncService).syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);
    }

    @Test
    void doAcceptTestWhenFailureSslCertificateSyncError() throws Exception {
        DBStack dbStack = getDBStack();
        dbStack.setCloudPlatform(CloudPlatform.AZURE.toString());
        addDatabaseServerToDBStack(dbStack);
        setupMocksMinimal(dbStack);

        when(userGeneratorService.updateUserName(ROOT_USER_NAME, Optional.of(CloudPlatform.AZURE), DB_HOST_NAME))
            .thenReturn(ROOT_USER_NAME + "@" + DB_SHORT_HOST_NAME);

        Exception e = new Exception();
        doThrow(e).when(databaseServerSslCertificateSyncService).syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        Selectable selectable = new ExceptionCatcherEventHandlerTestSupport<>(underTest).doAccept(event);

        verifyFailureEvent(e, selectable);

        verify(databaseServerConfigService).update(databaseServerConfigCaptor.capture());
        DatabaseServerConfig databaseServerConfig = databaseServerConfigCaptor.getValue();
        assertThat(databaseServerConfig.getHost()).isEqualTo(DB_HOST_NAME);
        assertThat(databaseServerConfig.getPort().intValue()).isEqualTo(PORT);

        assertThat(databaseServerConfig.getConnectionUserName()).isEqualTo(ROOT_USER_NAME + "@" + DB_SHORT_HOST_NAME);
    }

    private void setupMocksMinimal(DBStack dbStack) {
        UpdateDatabaseServerRegistrationRequest request = getRequest(dbStack);
        when(event.getData()).thenReturn(request);

        setupCloudResourceMock(DB_HOST_NAME, ResourceType.RDS_HOSTNAME);
        setupCloudResourceMock(Integer.toString(PORT), ResourceType.RDS_PORT);

        DatabaseServerConfig originalDatabaseServerConfig = getOriginalDatabaseServerConfig();
        when(databaseServerConfigService.getByCrn(CRN)).thenReturn(Optional.of(originalDatabaseServerConfig));
    }

    private void setupCloudResourceMock(String value, ResourceType type) {
        CloudResource resource = mock(CloudResource.class);
        when(resource.getName()).thenReturn(value);
        when(cloudResourceHelper.getResourceTypeFromList(eq(type), any())).thenReturn(Optional.of(resource));
    }

    private DBStack getDBStack() {
        DBStack dbStack = new DBStack();
        dbStack.setName(DB_STACK_NAME);
        dbStack.setEnvironmentId(ENVIRONMENT_CRN);
        dbStack.setResourceCrn(CRN.toString());
        return dbStack;
    }

    private void addDatabaseServerToDBStack(DBStack dbStack) {
        DatabaseServer databaseServer = new DatabaseServer();
        dbStack.setDatabaseServer(databaseServer);
        databaseServer.setAccountId(ACCOUNT_ID);
        databaseServer.setConnectionDriver(CONNECTION_DRIVER);
        databaseServer.setRootUserName(ROOT_USER_NAME);
        databaseServer.setRootPassword(ROOT_PASSWORD);
        databaseServer.setDatabaseVendor(DatabaseVendor.POSTGRES);
        databaseServer.setPort(PORT);
    }

    private DatabaseServerConfig getOriginalDatabaseServerConfig() {
        DatabaseServerConfig originalDatabaseServerConfig = new DatabaseServerConfig();
        originalDatabaseServerConfig.setHost(null);
        originalDatabaseServerConfig.setPort(null);
        originalDatabaseServerConfig.setConnectionUserName(ROOT_USER_NAME);
        return originalDatabaseServerConfig;
    }

    private UpdateDatabaseServerRegistrationRequest getRequest(DBStack dbStack) {
        when(cloudContext.getId()).thenReturn(RESOURCE_ID);
        return new UpdateDatabaseServerRegistrationRequest(cloudContext, cloudCredential, dbStack, databaseStack, List.of());
    }

}

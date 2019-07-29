package com.sequenceiq.redbeams.flow.redbeams.provision;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.redbeams.TestData;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.RegisterDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.handler.RegisterDatabaseServerHandler;
import com.sequenceiq.redbeams.repository.DatabaseServerConfigRepository;

import reactor.bus.Event;
import reactor.bus.EventBus;

public class RegisterDatabaseServerHandlerTest {

    private static final long DEFAULT_WORKSPACE_ID = 0L;

    private static final String ACCOUNT_ID = "accountId";

    private static final String DB_STACK_NAME = "dbStackName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String CONNECTION_DRIVER = "connectionDriver";

    private static final String ROOT_USER_NAME = "rootUserName";

    private static final String ROOT_PASSWORD = "rootPassword";

    private static final int PORT = 9753;

    private static final String DB_HOST_NAME = "dbHostName";

    private static final Crn CRN = TestData.getTestCrn("database", "name");

    @Mock
    private EventBus eventBus;

    @Mock
    private DatabaseServerConfigRepository databaseServerConfigRepository;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @InjectMocks
    private RegisterDatabaseServerHandler underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAccept() {
        Event event = mock(Event.class);
        DBStack dbStack = new DBStack();
        dbStack.setName(DB_STACK_NAME);
        dbStack.setEnvironmentId(ENVIRONMENT_CRN);
        dbStack.setResourceCrn(CRN);
        DatabaseServer databaseServer = new DatabaseServer();
        dbStack.setDatabaseServer(databaseServer);
        databaseServer.setAccountId(ACCOUNT_ID);
        databaseServer.setConnectionDriver(CONNECTION_DRIVER);
        databaseServer.setRootUserName(ROOT_USER_NAME);
        databaseServer.setRootPassword(ROOT_PASSWORD);
        databaseServer.setDatabaseVendor(DatabaseVendor.POSTGRES);
        databaseServer.setPort(PORT);
        setupCloudresourceMocks(DB_HOST_NAME, ResourceType.RDS_HOSTNAME);
        setupCloudresourceMocks("8642", ResourceType.RDS_PORT);
        RegisterDatabaseServerRequest registerDatabaseServerRequest = getRegisterDatabaseServerRequest(dbStack);
        when(event.getData()).thenReturn(registerDatabaseServerRequest);
        ArgumentCaptor<DatabaseServerConfig> databaseServerConfigCaptor = ArgumentCaptor.forClass(DatabaseServerConfig.class);

        underTest.accept(event);

        verify(databaseServerConfigRepository).save(databaseServerConfigCaptor.capture());
        DatabaseServerConfig databaseServerConfig = databaseServerConfigCaptor.getValue();
        assertEquals(DEFAULT_WORKSPACE_ID, databaseServerConfig.getWorkspaceId().longValue());
        assertEquals(ResourceStatus.SERVICE_MANAGED, databaseServerConfig.getResourceStatus());
        assertEquals(ACCOUNT_ID, databaseServerConfig.getAccountId());
        assertEquals(DB_STACK_NAME, databaseServerConfig.getName());
        assertEquals(DB_STACK_NAME, databaseServerConfig.getName());
        assertEquals(ENVIRONMENT_CRN, databaseServerConfig.getEnvironmentId());
        assertEquals(CONNECTION_DRIVER, databaseServerConfig.getConnectionDriver());
        assertEquals(ROOT_USER_NAME, databaseServerConfig.getConnectionUserName());
        assertEquals(ROOT_PASSWORD, databaseServerConfig.getConnectionPassword());
        assertEquals(DatabaseVendor.POSTGRES, databaseServerConfig.getDatabaseVendor());
//        assertEquals(PORT, databaseServerConfig.getPort().longValue());
        assertEquals(DB_HOST_NAME, databaseServerConfig.getHost());
        assertEquals(8642L, databaseServerConfig.getPort().longValue());
        assertEquals(CRN, databaseServerConfig.getResourceCrn());
        assertEquals(dbStack, databaseServerConfig.getDbStack().get());
    }

    private void setupCloudresourceMocks(String dbHostName2, ResourceType rdsHostname) {
        CloudResource dbHostName = mock(CloudResource.class);
        when(dbHostName.getName()).thenReturn(dbHostName2);
        when(cloudResourceHelper.getResourceTypeFromList(eq(rdsHostname), any())).thenReturn(Optional.of(dbHostName));
    }

    private RegisterDatabaseServerRequest getRegisterDatabaseServerRequest(DBStack dbStack) {
        RegisterDatabaseServerRequest registerDatabaseServerRequest = mock(RegisterDatabaseServerRequest.class);
        when(registerDatabaseServerRequest.getDbResources()).thenReturn(mock(List.class));
        when(registerDatabaseServerRequest.getDBStack()).thenReturn(dbStack);
        when(registerDatabaseServerRequest.getCloudContext()).thenReturn(mock(CloudContext.class));
        return registerDatabaseServerRequest;
    }
}

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
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.handler.UpdateDatabaseServerRegistrationHandler;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;

import reactor.bus.Event;
import reactor.bus.EventBus;

public class UpdateDatabaseServerRegistrationHandlerTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String DB_STACK_NAME = "dbStackName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String CONNECTION_DRIVER = "connectionDriver";

    private static final String ROOT_USER_NAME = "rootUserName";

    private static final String ROOT_PASSWORD = "rootPassword";

    private static final int PORT = 8642;

    private static final String DB_HOST_NAME = "dbHostName";

    private static final Crn CRN = TestData.getTestCrn("database", "name");

    @Mock
    private EventBus eventBus;

    @Mock
    private DatabaseServerConfigService databaseServerConfigService;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @InjectMocks
    private UpdateDatabaseServerRegistrationHandler underTest;

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
        UpdateDatabaseServerRegistrationRequest request = getRequest(dbStack);
        when(event.getData()).thenReturn(request);

        setupCloudresourceMocks(DB_HOST_NAME, ResourceType.RDS_HOSTNAME);
        setupCloudresourceMocks(Integer.toString(PORT), ResourceType.RDS_PORT);

        DatabaseServerConfig originalDatabaseServerConfig = new DatabaseServerConfig();
        originalDatabaseServerConfig.setHost(null);
        originalDatabaseServerConfig.setPort(null);
        when(databaseServerConfigService.getByCrn(CRN)).thenReturn(Optional.of(originalDatabaseServerConfig));

        underTest.accept(event);

        ArgumentCaptor<DatabaseServerConfig> databaseServerConfigCaptor = ArgumentCaptor.forClass(DatabaseServerConfig.class);
        verify(databaseServerConfigService).update(databaseServerConfigCaptor.capture());
        DatabaseServerConfig databaseServerConfig = databaseServerConfigCaptor.getValue();
        assertEquals(DB_HOST_NAME, databaseServerConfig.getHost());
        assertEquals(PORT, databaseServerConfig.getPort().intValue());
    }

    private void setupCloudresourceMocks(String dbHostName2, ResourceType rdsHostname) {
        CloudResource dbHostName = mock(CloudResource.class);
        when(dbHostName.getName()).thenReturn(dbHostName2);
        when(cloudResourceHelper.getResourceTypeFromList(eq(rdsHostname), any())).thenReturn(Optional.of(dbHostName));
    }

    private UpdateDatabaseServerRegistrationRequest getRequest(DBStack dbStack) {
        UpdateDatabaseServerRegistrationRequest request = mock(UpdateDatabaseServerRegistrationRequest.class);
        when(request.getDbResources()).thenReturn(mock(List.class));
        when(request.getDBStack()).thenReturn(dbStack);
        when(request.getCloudContext()).thenReturn(mock(CloudContext.class));
        return request;
    }
}

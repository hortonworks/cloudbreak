package com.sequenceiq.redbeams.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.common.cost.CostTagging;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;

@ExtendWith(MockitoExtension.class)
class RedbeamsCreationServiceTest {

    private static final Long DB_STACK_ID = 1234L;

    private static final String DB_STACK_NAME = "dbStackName";

    private static final String DB_STACK_DESCRIPTION = "dbStackDescription";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String CLOUD_PLATFORM = "cloudPlatform";

    private static final String PLATFORM_VARIANT = "platformVariant";

    private static final String ACCOUNT_ID = "accountId";

    private static final String CONNECTION_DRIVER = "connectionDriver";

    private static final String ROOT_USER_NAME = "rootUserName";

    private static final String ROOT_PASSWORD = "rootPassword";

    private static final int PORT = 8642;

    private static final String TEMPLATE = "template";

    @Mock
    private CrnService crnService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private DBStackService dbStackService;

    @Mock
    private CostTagging costTagging;

    @Mock
    private DatabaseServerConfigService databaseServerConfigService;

    @Mock
    private RedbeamsFlowManager flowManager;

    @InjectMocks
    private RedbeamsCreationService underTest;

    private DBStack dbStack;

    private CloudConnector<Object> connector;

    @BeforeEach
    public void setup() throws Exception {
        dbStack = new DBStack();
        // this wouldn't really be set before launchDatabaseServer is called
        dbStack.setId(DB_STACK_ID);
        dbStack.setName(DB_STACK_NAME);
        dbStack.setDescription(DB_STACK_DESCRIPTION);
        dbStack.setEnvironmentId(ENVIRONMENT_CRN);
        dbStack.setCloudPlatform(CLOUD_PLATFORM);
        dbStack.setPlatformVariant(PLATFORM_VARIANT);
        dbStack.setUserName("username");
        dbStack.setOwnerCrn(Crn.fromString("crn:cdp:iam:us-west-1:1234:user:234123"));
        dbStack.setResourceCrn(Crn.fromString("crn:cdp:iam:us-west-1:1234:database:2312312"));
        DatabaseServer databaseServer = new DatabaseServer();
        dbStack.setDatabaseServer(databaseServer);
        databaseServer.setAccountId(ACCOUNT_ID);
        databaseServer.setConnectionDriver(CONNECTION_DRIVER);
        databaseServer.setRootUserName(ROOT_USER_NAME);
        databaseServer.setRootPassword(ROOT_PASSWORD);
        databaseServer.setDatabaseVendor(DatabaseVendor.POSTGRES);
        databaseServer.setPort(PORT);

        connector = mock(CloudConnector.class, RETURNS_DEEP_STUBS);
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(connector);
        when(connector.resources().getDBStackTemplate()).thenReturn(TEMPLATE);
    }

    @Test
    public void testLaunchDatabaseServer() {
        Crn crn = Crn.fromString("crn:cdp:iam:us-west-1:1234:database:2312312");
        when(dbStackService.findByNameAndEnvironmentCrn(DB_STACK_NAME, ENVIRONMENT_CRN)).thenReturn(Optional.empty());
        when(dbStackService.save(dbStack)).thenReturn(dbStack);

        DBStack launchedStack = underTest.launchDatabaseServer(dbStack);
        assertEquals(dbStack, launchedStack);
        verify(dbStackService).save(dbStack);

        assertEquals(crn, dbStack.getResourceCrn());
        assertEquals(TEMPLATE, dbStack.getTemplate());

        ArgumentCaptor<DatabaseServerConfig> databaseServerConfigCaptor = ArgumentCaptor.forClass(DatabaseServerConfig.class);
        verify(databaseServerConfigService).create(databaseServerConfigCaptor.capture(), eq(RedbeamsCreationService.DEFAULT_WORKSPACE), eq(false));
        DatabaseServerConfig databaseServerConfig = databaseServerConfigCaptor.getValue();
        assertEquals(ResourceStatus.SERVICE_MANAGED, databaseServerConfig.getResourceStatus());
        assertEquals(ACCOUNT_ID, databaseServerConfig.getAccountId());
        assertEquals(DB_STACK_NAME, databaseServerConfig.getName());
        assertEquals(DB_STACK_DESCRIPTION, databaseServerConfig.getDescription());
        assertEquals(ENVIRONMENT_CRN, databaseServerConfig.getEnvironmentId());
        assertEquals(CONNECTION_DRIVER, databaseServerConfig.getConnectionDriver());
        assertEquals(ROOT_USER_NAME, databaseServerConfig.getConnectionUserName());
        assertEquals(ROOT_PASSWORD, databaseServerConfig.getConnectionPassword());
        assertEquals(DatabaseVendor.POSTGRES, databaseServerConfig.getDatabaseVendor());
        assertNull(databaseServerConfig.getHost());
        assertNull(databaseServerConfig.getPort());
        assertEquals(crn, databaseServerConfig.getResourceCrn());
        assertEquals(dbStack, databaseServerConfig.getDbStack().get());

        ArgumentCaptor<RedbeamsEvent> eventCaptor = ArgumentCaptor.forClass(RedbeamsEvent.class);
        verify(flowManager).notify(eq(RedbeamsProvisionEvent.REDBEAMS_PROVISION_EVENT.selector()), eventCaptor.capture());
        RedbeamsEvent provisionEvent = eventCaptor.getValue();
        assertEquals(RedbeamsProvisionEvent.REDBEAMS_PROVISION_EVENT.selector(), provisionEvent.selector());
        assertEquals(dbStack.getId(), provisionEvent.getResourceId());
    }

}

package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureFlexibleServerClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureRDSAutoMigrationException;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.common.model.AzureDatabaseType;

@ExtendWith(MockitoExtension.class)
public class AzureRDSAutoMigrationValidatorTest {
    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @InjectMocks
    private AzureRDSAutoMigrationValidator underTest;

    @Mock
    private AzureClient azureClient;

    @Mock
    private AzureFlexibleServerClient azureFlexibleServerClient;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DatabaseServer databaseServer;

    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    void setup() {
        authenticatedContext = new AuthenticatedContext(cloudContext, null);
        authenticatedContext.putParameter(AzureClient.class, azureClient);
        lenient().when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        lenient().when(databaseServer.getServerId()).thenReturn("serverId");
        lenient().when(azureClient.getFlexibleServerClient()).thenReturn(azureFlexibleServerClient);
    }

    @Test
    void testValidateWhenSingleAndNoFlexibleServer() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn("rg");

        underTest.validate(authenticatedContext, databaseStack);

        verify(azureFlexibleServerClient, times(1)).getFlexibleServer("rg", "serverId");
    }

    @Test
    void testValidateWhenSingleAndFlexibleServerFound() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn("rg");
        Server server = mock(Server.class);
        when(server.id()).thenReturn("serverId");
        when(azureFlexibleServerClient.getFlexibleServer("rg", "serverId")).thenReturn(server);

        AzureRDSAutoMigrationException azureRDSAutoMigrationException = assertThrows(AzureRDSAutoMigrationException.class,
                () -> underTest.validate(authenticatedContext, databaseStack));

        assertTrue(azureRDSAutoMigrationException.getAzureRDSAutoMigrationParams().isPresent());
        assertEquals("serverId", azureRDSAutoMigrationException.getAzureRDSAutoMigrationParams().get().serverId());
        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, azureRDSAutoMigrationException.getAzureRDSAutoMigrationParams().get().azureDatabaseType());
    }

    @Test
    void testValidateWhenSingleAndException() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn("rg");
        when(azureFlexibleServerClient.getFlexibleServer("rg", "serverId")).thenThrow(new RuntimeException("ex"));

        underTest.validate(authenticatedContext, databaseStack);

        verify(azureFlexibleServerClient, times(1)).getFlexibleServer("rg", "serverId");
    }

    @Test
    void testValidateWhenFlexibleServer() {
        when(databaseServer.getStringParameter(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY)).thenReturn(AzureDatabaseType.FLEXIBLE_SERVER.name());

        underTest.validate(authenticatedContext, databaseStack);

        verify(azureResourceGroupMetadataProvider, never()).getResourceGroupName(any(), any(DatabaseStack.class));
        verify(azureFlexibleServerClient, never()).getFlexibleServer(anyString(), anyString());
    }
}

package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAzureRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.common.model.AzureDatabaseType;

@ExtendWith(MockitoExtension.class)
class DatabaseRequestToDatabaseConverterTest {
    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private DatabaseRequestToDatabaseConverter service;

    @Test
    void testConvertWithAzureSingleServer() {
        DatabaseRequest databaseRequest = new DatabaseRequest();
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.NON_HA);

        Database database = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> service.convert(CloudPlatform.AZURE, databaseRequest));

        assertEquals(AzureDatabaseType.SINGLE_SERVER.name(), database.getAttributes().getMap().get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY));
        assertNull(database.getDatalakeDatabaseAvailabilityType());
    }

    @Test
    void testConvertWithAws() {
        DatabaseRequest databaseRequest = new DatabaseRequest();

        Database database = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> service.convert(CloudPlatform.AWS, databaseRequest));

        assertNull(database.getAttributes());
        assertEquals(DatabaseAvailabilityType.NONE, database.getExternalDatabaseAvailabilityType());
        assertNull(database.getDatalakeDatabaseAvailabilityType());
    }

    @Test
    void testConvertWithAzureFlexibleServer() {
        DatabaseRequest databaseRequest = new DatabaseRequest();
        DatabaseAzureRequest databaseAzureRequest = new DatabaseAzureRequest();
        databaseAzureRequest.setAzureDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER);
        databaseRequest.setDatabaseAzureRequest(databaseAzureRequest);
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.HA);
        when(entitlementService.isAzureDatabaseFlexibleServerEnabled(anyString())).thenReturn(true);

        Database database = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> service.convert(CloudPlatform.AZURE, databaseRequest));

        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER.name(), database.getAttributes().getMap().get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY));
    }

    @Test
    void testConvertWithAzureFlexibleServerWhenNotAllowed() {
        DatabaseRequest databaseRequest = new DatabaseRequest();
        DatabaseAzureRequest databaseAzureRequest = new DatabaseAzureRequest();
        databaseAzureRequest.setAzureDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER);
        databaseRequest.setDatabaseAzureRequest(databaseAzureRequest);
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.NON_HA);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(ACTOR, () -> service.convert(CloudPlatform.AZURE, databaseRequest)));

        assertTrue(exception.getMessage().contains("You are not entitled to use Flexible Database Server on Azure for your cluster."));
    }

    @Test
    void testMapAzureAttributesInCaseOfEmbedded() {
        DatabaseRequest databaseRequest = new DatabaseRequest();
        DatabaseAzureRequest databaseAzureRequest = new DatabaseAzureRequest();
        databaseAzureRequest.setAzureDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER);
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.NONE);
        databaseRequest.setDatabaseAzureRequest(databaseAzureRequest);

        Database result = service.convert(CloudPlatform.AZURE, databaseRequest);

        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER.name(), result.getAttributes().getMap().get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY));
        assertEquals(DatabaseAvailabilityType.NONE, result.getExternalDatabaseAvailabilityType());
        assertNull(result.getDatalakeDatabaseAvailabilityType());
    }
}

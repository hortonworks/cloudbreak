package com.sequenceiq.datalake.service.sdx.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAzureRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.sdx.api.model.SdxDatabaseAzureRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@ExtendWith(MockitoExtension.class)
public class AzureDatabaseAttributesServiceTest {
    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    private static final String ACCOUNT_ID = "altus";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private AzureDatabaseAttributesService service;

    @Test
    void testGetAzureDatabaseTypeWhenAttributesNull() {
        SdxDatabase sdxDatabase = new SdxDatabase();

        AzureDatabaseType azureDatabaseType = service.getAzureDatabaseType(sdxDatabase);
        assertEquals(AzureDatabaseType.SINGLE_SERVER, azureDatabaseType);
    }

    @Test
    void testGetAzureDatabaseTypeSingleServer() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setAttributes(new Json("{\"AZURE_DATABASE_TYPE\": \"SINGLE_SERVER\"}"));

        AzureDatabaseType azureDatabaseType = service.getAzureDatabaseType(sdxDatabase);
        assertEquals(AzureDatabaseType.SINGLE_SERVER, azureDatabaseType);
    }

    @Test
    void testGetAzureDatabaseTypeFlexibleServer() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setAttributes(new Json("{\"AZURE_DATABASE_TYPE\": \"FLEXIBLE_SERVER\"}"));

        AzureDatabaseType azureDatabaseType = service.getAzureDatabaseType(sdxDatabase);
        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, azureDatabaseType);
    }

    @Test
    void testGetAzureDatabaseTypeInvalid() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setAttributes(new Json("{\"AZURE_DATABASE_TYPE\": \"invlaid\"}"));

        AzureDatabaseType azureDatabaseType = service.getAzureDatabaseType(sdxDatabase);
        assertEquals(AzureDatabaseType.SINGLE_SERVER, azureDatabaseType);
    }

    @Test
    void testGetAzureDatabaseTypeNoType() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setAttributes(new Json("{}"));

        AzureDatabaseType azureDatabaseType = service.getAzureDatabaseType(sdxDatabase);
        assertEquals(AzureDatabaseType.SINGLE_SERVER, azureDatabaseType);
    }

    @Test
    void testConfigureAzureDatabaseWithSingleServer() {
        SdxDatabaseRequest databaseRequest = new SdxDatabaseRequest();
        SdxDatabase sdxDatabase = new SdxDatabase();
        ThreadBasedUserCrnProvider.doAs(ACTOR, () -> service.configureAzureDatabase(null, databaseRequest, sdxDatabase));

        assertEquals(AzureDatabaseType.SINGLE_SERVER, service.getAzureDatabaseType(sdxDatabase));
    }

    @Test
    void testConfigureAzureDatabaseWithFlexibleServer() {
        SdxDatabaseRequest databaseRequest = new SdxDatabaseRequest();
        SdxDatabaseAzureRequest azureRequest = new SdxDatabaseAzureRequest();
        azureRequest.setAzureDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER);
        databaseRequest.setSdxDatabaseAzureRequest(azureRequest);

        SdxDatabase sdxDatabase = new SdxDatabase();
        when(entitlementService.isAzureDatabaseFlexibleServerEnabled(anyString())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(ACTOR, () -> service.configureAzureDatabase(null, databaseRequest, sdxDatabase));

        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, service.getAzureDatabaseType(sdxDatabase));
    }

    @Test
    void testConfigureAzureDatabaseWithFlexibleServerInternal() {
        DatabaseRequest internalDatabaseRequest = new DatabaseRequest();
        DatabaseAzureRequest azureRequest = new DatabaseAzureRequest();
        azureRequest.setAzureDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER);
        internalDatabaseRequest.setDatabaseAzureRequest(azureRequest);

        SdxDatabase sdxDatabase = new SdxDatabase();
        when(entitlementService.isAzureDatabaseFlexibleServerEnabled(anyString())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(ACTOR, () -> service.configureAzureDatabase(internalDatabaseRequest, null, sdxDatabase));

        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, service.getAzureDatabaseType(sdxDatabase));
    }

    @Test
    void testConfigureAzureDatabaseWithFlexibleServerNotAllowed() {
        SdxDatabaseRequest databaseRequest = new SdxDatabaseRequest();
        SdxDatabaseAzureRequest azureRequest = new SdxDatabaseAzureRequest();
        azureRequest.setAzureDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER);
        databaseRequest.setSdxDatabaseAzureRequest(azureRequest);

        SdxDatabase sdxDatabase = new SdxDatabase();
        when(entitlementService.isAzureDatabaseFlexibleServerEnabled(anyString())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(ACTOR, () -> service.configureAzureDatabase(null, databaseRequest, sdxDatabase)));

        assertEquals("You are not entitled to use Flexible Database Server on Azure for your cluster." +
                " Please contact Cloudera to enable " + Entitlement.CDP_AZURE_DATABASE_FLEXIBLE_SERVER + " for your account", exception.getMessage());
    }

}

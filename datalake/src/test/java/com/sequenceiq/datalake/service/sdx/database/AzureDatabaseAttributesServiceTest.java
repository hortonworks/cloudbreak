package com.sequenceiq.datalake.service.sdx.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAzureRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.database.EnvironmentDatabaseService;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.sdx.api.model.SdxDatabaseAzureRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@ExtendWith(MockitoExtension.class)
public class AzureDatabaseAttributesServiceTest {
    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    private static final String ACCOUNT_ID = "altus";

    @Mock
    private EnvironmentDatabaseService environmentDatabaseService;

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
    void testDetermineAzureDatabaseTypeWithNoDB() {
        SdxDatabaseRequest databaseRequest = new SdxDatabaseRequest();
        when(environmentDatabaseService.validateOrModifyDatabaseTypeIfNeeded(isNull(AzureDatabaseType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AzureDatabaseType actualResult = ThreadBasedUserCrnProvider.doAs(
                ACTOR, () -> service.determineAzureDatabaseType(null, databaseRequest));

        assertNull(actualResult);
    }

    @Test
    void testDetermineAzureDatabaseTypeWithSingleServer() {
        SdxDatabaseRequest databaseRequest = new SdxDatabaseRequest();
        SdxDatabaseAzureRequest azureRequest = new SdxDatabaseAzureRequest();
        azureRequest.setAzureDatabaseType(AzureDatabaseType.SINGLE_SERVER);
        databaseRequest.setSdxDatabaseAzureRequest(azureRequest);
        when(environmentDatabaseService.validateOrModifyDatabaseTypeIfNeeded(any(AzureDatabaseType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AzureDatabaseType actualResult = ThreadBasedUserCrnProvider.doAs(
                ACTOR, () -> service.determineAzureDatabaseType(null, databaseRequest));

        assertEquals(AzureDatabaseType.SINGLE_SERVER, actualResult);
    }

    @Test
    void testDetermineAzureDatabaseTypeWithFlexibleServer() {
        SdxDatabaseRequest databaseRequest = new SdxDatabaseRequest();
        SdxDatabaseAzureRequest azureRequest = new SdxDatabaseAzureRequest();
        azureRequest.setAzureDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER);
        databaseRequest.setSdxDatabaseAzureRequest(azureRequest);
        when(environmentDatabaseService.validateOrModifyDatabaseTypeIfNeeded(any(AzureDatabaseType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AzureDatabaseType actualResult = ThreadBasedUserCrnProvider.doAs(
                ACTOR, () -> service.determineAzureDatabaseType(null, databaseRequest));

        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, actualResult);
    }

    @Test
    void testDetermineAzureDatabaseTypeWithFlexibleServerInternal() {
        DatabaseRequest internalDatabaseRequest = new DatabaseRequest();
        DatabaseAzureRequest azureRequest = new DatabaseAzureRequest();
        azureRequest.setAzureDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER);
        internalDatabaseRequest.setDatabaseAzureRequest(azureRequest);
        when(environmentDatabaseService.validateOrModifyDatabaseTypeIfNeeded(any(AzureDatabaseType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AzureDatabaseType actualResult = ThreadBasedUserCrnProvider.doAs(
                ACTOR, () -> service.determineAzureDatabaseType(internalDatabaseRequest, null));

        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, actualResult);
    }

    @Test
    void testConfigureAzureDatabaseWithNullDbType() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        service.configureAzureDatabase(null, null, null, sdxDatabase);
        assertNull(sdxDatabase.getAttributes().getMap().get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY));
    }

    @Test
    void testConfigureAzureDatabaseWithDbTypeGiven() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        service.configureAzureDatabase(AzureDatabaseType.FLEXIBLE_SERVER, null, null, sdxDatabase);
        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, service.getAzureDatabaseType(sdxDatabase));
    }

    @Test
    void testUpdateVersionRelatedDatabaseParams() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setAttributes(new Json("{\"AZURE_DATABASE_TYPE\": \"SINGLE_SERVER\"}"));
        Optional<SdxDatabase> actualDb = service.updateVersionRelatedDatabaseParams(sdxDatabase, MajorVersion.VERSION_14.getMajorVersion());
        assertTrue(actualDb.isPresent());
        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, service.getAzureDatabaseType(sdxDatabase));
    }

    @Test
    void testUpdateVersionRelatedDatabaseParamsNoUpdateNeeded() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setAttributes(new Json("{\"AZURE_DATABASE_TYPE\": \"FLEXIBLE_SERVER\"}"));
        Optional<SdxDatabase> actualDb = service.updateVersionRelatedDatabaseParams(sdxDatabase, MajorVersion.VERSION_14.getMajorVersion());
        assertFalse(actualDb.isPresent());
    }

    @Test
    void testUpdateVersionRelatedDatabaseParamsNoUpdateNeededVersion() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setAttributes(new Json("{\"AZURE_DATABASE_TYPE\": \"SINGLE_SERVER\"}"));
        Optional<SdxDatabase> actualDb = service.updateVersionRelatedDatabaseParams(sdxDatabase, MajorVersion.VERSION_11.getMajorVersion());
        assertFalse(actualDb.isPresent());
    }
}

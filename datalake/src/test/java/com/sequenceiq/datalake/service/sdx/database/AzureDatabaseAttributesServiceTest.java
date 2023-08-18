package com.sequenceiq.datalake.service.sdx.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxDatabase;

@ExtendWith(MockitoExtension.class)
public class AzureDatabaseAttributesServiceTest {
    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

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
        SdxDatabase sdxDatabase = new SdxDatabase();
        when(entitlementService.isAzureDatabaseFlexibleServerEnabled(anyString())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(ACTOR, () -> service.configureAzureDatabase(sdxDatabase));

        assertEquals(AzureDatabaseType.SINGLE_SERVER, service.getAzureDatabaseType(sdxDatabase));
    }

    @Test
    void testConfigureAzureDatabaseWithFlexibleServer() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        when(entitlementService.isAzureDatabaseFlexibleServerEnabled(anyString())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(ACTOR, () -> service.configureAzureDatabase(sdxDatabase));

        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, service.getAzureDatabaseType(sdxDatabase));
    }

}

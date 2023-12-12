package com.sequenceiq.cloudbreak.service.database;

import static com.sequenceiq.common.model.AzureDatabaseType.FLEXIBLE_SERVER;
import static com.sequenceiq.common.model.AzureDatabaseType.SINGLE_SERVER;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
class EnvironmentDatabaseServiceTest {
    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private EnvironmentDatabaseService underTest;

    @Test
    void testValidateWithAzureFlexibleServerWhenNotAllowed() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(ACTOR,
                        () -> underTest.validateOrModifyDatabaseTypeIfNeeded(environmentResponse, FLEXIBLE_SERVER)));

        assertTrue(exception.getMessage().contains("You are not entitled to use Flexible Database Server on Azure for your cluster."));
    }

    @Test
    void testSingleServerFallbackWhenPrivateSingleSetup() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentResponse.setNetwork(environmentNetworkResponse);
        environmentNetworkResponse.setServiceEndpointCreation(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT);
        environmentNetworkResponse.setAzure(new EnvironmentNetworkAzureParams());

        AzureDatabaseType actualResult = ThreadBasedUserCrnProvider.doAs(ACTOR,
                () -> underTest.validateOrModifyDatabaseTypeIfNeeded(environmentResponse, null));

        assertEquals(SINGLE_SERVER, actualResult);
    }

    @Test
    void testSingleServerFallbackWhenPrivateSingleSetupAndSingleServerGiven() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentResponse.setNetwork(environmentNetworkResponse);
        environmentNetworkResponse.setServiceEndpointCreation(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT);
        environmentNetworkResponse.setAzure(new EnvironmentNetworkAzureParams());
        when(entitlementService.isAzureDatabaseFlexibleServerEnabled(anyString())).thenReturn(true);

        AzureDatabaseType actualResult = ThreadBasedUserCrnProvider.doAs(ACTOR,
                () -> underTest.validateOrModifyDatabaseTypeIfNeeded(environmentResponse, SINGLE_SERVER));

        assertEquals(SINGLE_SERVER, actualResult);
    }

    @Test
    void testValidateWhenPrivateSingleSetupAndFlexibleDbTypeShouldThrowBadRequest() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentResponse.setNetwork(environmentNetworkResponse);
        EnvironmentNetworkAzureParams environmentNetworkAzureParams = new EnvironmentNetworkAzureParams();
        environmentNetworkAzureParams.setDatabasePrivateDnsZoneId("zoneid");
        environmentNetworkResponse.setAzure(environmentNetworkAzureParams);
        when(entitlementService.isAzureDatabaseFlexibleServerEnabled(anyString())).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.validateOrModifyDatabaseTypeIfNeeded(environmentResponse, FLEXIBLE_SERVER)));

        assertTrue(exception.getMessage().contains("Your environment was created with Azure Private Single Server database setup."));
    }

    @Test
    void testValidateWhenFlexibleEnabledAndNoDBTypeGiven() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        when(entitlementService.isAzureDatabaseFlexibleServerEnabled(anyString())).thenReturn(true);

        AzureDatabaseType actualResult = ThreadBasedUserCrnProvider.doAs(ACTOR,
                () -> underTest.validateOrModifyDatabaseTypeIfNeeded(environmentResponse, null));

        assertEquals(FLEXIBLE_SERVER, actualResult);
    }

    @Test
    void testValidateWhenFlexibleDisabledAndNoDBTypeGiven() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();

        AzureDatabaseType actualResult = ThreadBasedUserCrnProvider.doAs(ACTOR,
                () -> underTest.validateOrModifyDatabaseTypeIfNeeded(environmentResponse, null));

        assertEquals(SINGLE_SERVER, actualResult);
    }
}

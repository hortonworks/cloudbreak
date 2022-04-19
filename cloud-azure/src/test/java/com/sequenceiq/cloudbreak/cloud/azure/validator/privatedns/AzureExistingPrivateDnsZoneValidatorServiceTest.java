package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;

import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.NETWORK_NAME;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.NETWORK_RESOURCE_GROUP_NAME;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.getPrivateDnsZoneResourceId;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.arm.resources.ResourceId;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.validator.ValidationTestUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(MockitoExtension.class)
public class AzureExistingPrivateDnsZoneValidatorServiceTest {

    @Mock
    private AzurePrivateDnsZoneValidatorService azurePrivateDnsZoneValidatorService;

    @InjectMocks
    private AzureExistingPrivateDnsZoneValidatorService underTest;

    @Mock
    private AzureClient azureClient;

    @Test
    void testValidate() {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        ResourceId privateDnsZoneId = PrivateDnsZoneValidationTestConstants.getPrivateDnsZoneResourceId();
        Map<AzurePrivateDnsZoneServiceEnum, String> serviceToPrivateDnsZoneId = Map.of(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZoneId.id());

        resultBuilder = underTest.validate(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME, serviceToPrivateDnsZoneId, resultBuilder);

        assertFalse(resultBuilder.build().hasError());
        verify(azurePrivateDnsZoneValidatorService).existingPrivateDnsZoneNameIsSupported(eq(AzurePrivateDnsZoneServiceEnum.POSTGRES), any(),
                eq(resultBuilder));
        verify(azurePrivateDnsZoneValidatorService).privateDnsZoneExists(eq(azureClient), any(), eq(resultBuilder));
        verify(azurePrivateDnsZoneValidatorService).privateDnsZoneConnectedToNetwork(eq(azureClient), eq(NETWORK_RESOURCE_GROUP_NAME), eq(NETWORK_NAME), any(),
                eq(resultBuilder));
    }

    @Test
    void testValidateWhenInvalidPrivateDnsZoneResourceId() {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        String privateDnsZoneId = "invalidPrivateDnsZoneId";
        Map<AzurePrivateDnsZoneServiceEnum, String> serviceToPrivateDnsZoneId = Map.of(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZoneId);

        resultBuilder = underTest.validate(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME, serviceToPrivateDnsZoneId, resultBuilder);

        ValidationTestUtil.checkErrorsPresent(resultBuilder, List.of("The provided private DNS zone id invalidPrivateDnsZoneId for service " +
                "Microsoft.DBforPostgreSQL/servers is not a valid azure resource id."));
        verify(azurePrivateDnsZoneValidatorService, never()).existingPrivateDnsZoneNameIsSupported(any(), any(), eq(resultBuilder));
        verify(azurePrivateDnsZoneValidatorService, never()).privateDnsZoneExists(any(), any(), any());
        verify(azurePrivateDnsZoneValidatorService, never()).privateDnsZoneConnectedToNetwork(any(), anyString(), anyString(), any(), any());
    }

    @Test
    void testValidateWhenValidAndInvalidPrivateDnsZoneResourceId() {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        ResourceId privateDnsZoneIdPostgres = PrivateDnsZoneValidationTestConstants.getPrivateDnsZoneResourceId();
        String privateDnsZoneIdStorage = "invalidPrivateDnsZoneId";
        Map<AzurePrivateDnsZoneServiceEnum, String> serviceToPrivateDnsZoneId = Map.of(
                AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZoneIdPostgres.id(),
                AzurePrivateDnsZoneServiceEnum.STORAGE, privateDnsZoneIdStorage
        );
        when(azurePrivateDnsZoneValidatorService.existingPrivateDnsZoneNameIsSupported(any(), any(), any())).thenAnswer(invocation -> {
            ValidationResult.ValidationResultBuilder validationResultBuilder = invocation.getArgument(2);
            ResourceId privateDnsZoneId = invocation.getArgument(1);
            if (privateDnsZoneId.id().equals(privateDnsZoneIdStorage)) {
                throw new InvalidParameterException();
            }
            return validationResultBuilder;
        });

        resultBuilder = underTest.validate(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME, serviceToPrivateDnsZoneId, resultBuilder);

        ValidationTestUtil.checkErrorsPresent(resultBuilder, List.of("The provided private DNS zone id invalidPrivateDnsZoneId for service " +
                "Microsoft.Storage/storageAccounts is not a valid azure resource id."));
        verify(azurePrivateDnsZoneValidatorService).existingPrivateDnsZoneNameIsSupported(eq(AzurePrivateDnsZoneServiceEnum.POSTGRES), any(),
                eq(resultBuilder));
        verify(azurePrivateDnsZoneValidatorService).privateDnsZoneExists(eq(azureClient), any(), eq(resultBuilder));
        verify(azurePrivateDnsZoneValidatorService).privateDnsZoneConnectedToNetwork(eq(azureClient), eq(NETWORK_RESOURCE_GROUP_NAME), eq(NETWORK_NAME),
                any(), eq(resultBuilder));

        verify(azurePrivateDnsZoneValidatorService, never()).existingPrivateDnsZoneNameIsSupported(eq(AzurePrivateDnsZoneServiceEnum.STORAGE), any(), any());
    }

}

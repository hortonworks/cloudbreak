package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;

import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.NETWORK_NAME;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.NETWORK_RESOURCE_GROUP_NAME;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.getPrivateDnsZoneId;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
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
        String privateDnsZoneId = getPrivateDnsZoneId();
        Map<AzurePrivateDnsZoneServiceEnum, String> serviceToPrivateDnsZoneId = Map.of(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZoneId);

        resultBuilder = underTest.validate(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME, serviceToPrivateDnsZoneId, resultBuilder);

        assertFalse(resultBuilder.build().hasError());
        verify(azurePrivateDnsZoneValidatorService).existingPrivateDnsZoneNameIsSupported(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZoneId,
                resultBuilder);
        verify(azurePrivateDnsZoneValidatorService).privateDnsZoneExists(azureClient, privateDnsZoneId, resultBuilder);
        verify(azurePrivateDnsZoneValidatorService).privateDnsZoneConnectedToNetwork(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME, privateDnsZoneId,
                resultBuilder);
    }

    @Test
    void testValidateWhenInvalidPrivateDnsZoneResourceId() {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        String privateDnsZoneId = "invalidPrivateDnsZoneId";
        Map<AzurePrivateDnsZoneServiceEnum, String> serviceToPrivateDnsZoneId = Map.of(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZoneId);
        when(azurePrivateDnsZoneValidatorService.existingPrivateDnsZoneNameIsSupported(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZoneId, resultBuilder))
                .thenThrow(new InvalidParameterException());

        resultBuilder = underTest.validate(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME, serviceToPrivateDnsZoneId, resultBuilder);

        ValidationTestUtil.checkErrorsPresent(resultBuilder, List.of("The provided private DNS zone id invalidPrivateDnsZoneId for service " +
                "Microsoft.DBforPostgreSQL/servers is not a valid azure resource id."));
        verify(azurePrivateDnsZoneValidatorService).existingPrivateDnsZoneNameIsSupported(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZoneId,
                resultBuilder);
        verify(azurePrivateDnsZoneValidatorService, never()).privateDnsZoneExists(azureClient, privateDnsZoneId, resultBuilder);
        verify(azurePrivateDnsZoneValidatorService, never()).privateDnsZoneConnectedToNetwork(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME,
                privateDnsZoneId, resultBuilder);
    }

    @Test
    void testValidateWhenValidAndInvalidPrivateDnsZoneResourceId() {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        String privateDnsZoneIdPostgres = getPrivateDnsZoneId();
        String privateDnsZoneIdStorage = "invalidPrivateDnsZoneId";
        Map<AzurePrivateDnsZoneServiceEnum, String> serviceToPrivateDnsZoneId = Map.of(
                AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZoneIdPostgres,
                AzurePrivateDnsZoneServiceEnum.STORAGE, privateDnsZoneIdStorage
        );
        when(azurePrivateDnsZoneValidatorService.existingPrivateDnsZoneNameIsSupported(any(), any(), any())).thenAnswer(invocation -> {
            ValidationResult.ValidationResultBuilder validationResultBuilder = invocation.getArgument(2);
            String privateDnsZoneId = invocation.getArgument(1);
            if (privateDnsZoneId.equals(privateDnsZoneIdStorage)) {
                throw new InvalidParameterException();
            }
            return validationResultBuilder;
        });

        resultBuilder = underTest.validate(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME, serviceToPrivateDnsZoneId, resultBuilder);

        ValidationTestUtil.checkErrorsPresent(resultBuilder, List.of("The provided private DNS zone id invalidPrivateDnsZoneId for service " +
                "Microsoft.Storage/storageAccounts is not a valid azure resource id."));
        verify(azurePrivateDnsZoneValidatorService).existingPrivateDnsZoneNameIsSupported(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZoneIdPostgres,
                resultBuilder);
        verify(azurePrivateDnsZoneValidatorService).privateDnsZoneExists(azureClient, privateDnsZoneIdPostgres, resultBuilder);
        verify(azurePrivateDnsZoneValidatorService).privateDnsZoneConnectedToNetwork(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME,
                privateDnsZoneIdPostgres, resultBuilder);

        verify(azurePrivateDnsZoneValidatorService).existingPrivateDnsZoneNameIsSupported(AzurePrivateDnsZoneServiceEnum.STORAGE, privateDnsZoneIdStorage,
                resultBuilder);
        verify(azurePrivateDnsZoneValidatorService, never()).privateDnsZoneExists(azureClient, privateDnsZoneIdStorage, resultBuilder);
        verify(azurePrivateDnsZoneValidatorService, never()).privateDnsZoneConnectedToNetwork(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME,
                privateDnsZoneIdStorage, resultBuilder);

    }

}

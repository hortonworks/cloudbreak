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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneDescriptor;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.validator.ValidationTestUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(MockitoExtension.class)
public class AzureExistingPrivateDnsZoneValidatorServiceTest {

    private static final String AZURE_RESOURCE_ID_TEMPLATE =
            "/subscriptions/subscriptionid/resourceGroups/rgname/providers/Microsoft.Network/privateDnsZones/%s";

    private static final String VALID_PRIVATE_DNS_ZONE_ID = String.format(AZURE_RESOURCE_ID_TEMPLATE, "validPrivateDnsZoneId");

    private static final String INVALID_PRIVATE_DNS_ZONE_ID = "invalidPrivateDnsZoneId";

    @Mock
    private AzurePrivateDnsZoneValidatorService azurePrivateDnsZoneValidatorService;

    @InjectMocks
    private AzureExistingPrivateDnsZoneValidatorService underTest;

    @Mock
    private AzureClient azureClient;

    @Test
    void testValidate() {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        ResourceId privateDnsZoneId = getPrivateDnsZoneResourceId();
        AzurePrivateDnsZoneDescriptor azurePrivateDnsZoneDescriptor = new AzurePrivateDnsZoneDescriptorTestImpl("privateDnsZoneService");
        Map<AzurePrivateDnsZoneDescriptor, String> serviceToPrivateDnsZoneId = Map.of(azurePrivateDnsZoneDescriptor, privateDnsZoneId.id());

        resultBuilder = underTest.validate(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME, serviceToPrivateDnsZoneId, resultBuilder);

        assertFalse(resultBuilder.build().hasError());
        verify(azurePrivateDnsZoneValidatorService).existingPrivateDnsZoneNameIsSupported(eq(azurePrivateDnsZoneDescriptor), any(),
                eq(resultBuilder));
        verify(azurePrivateDnsZoneValidatorService).privateDnsZoneExists(eq(azureClient), any(), eq(resultBuilder));
        verify(azurePrivateDnsZoneValidatorService).privateDnsZoneConnectedToNetwork(eq(azureClient), eq(NETWORK_RESOURCE_GROUP_NAME), eq(NETWORK_NAME), any(),
                eq(resultBuilder));
    }

    @Test
    void testValidateWhenInvalidPrivateDnsZoneResourceId() {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        AzurePrivateDnsZoneDescriptor azurePrivateDnsZoneDescriptor = new AzurePrivateDnsZoneDescriptorTestImpl("privateDnsZoneService");
        Map<AzurePrivateDnsZoneDescriptor, String> serviceToPrivateDnsZoneId = Map.of(azurePrivateDnsZoneDescriptor, INVALID_PRIVATE_DNS_ZONE_ID);

        resultBuilder = underTest.validate(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME, serviceToPrivateDnsZoneId, resultBuilder);

        ValidationTestUtil.checkErrorsPresent(resultBuilder, List.of("The provided private DNS zone id invalidPrivateDnsZoneId for service " +
                "privateDnsZoneService is not a valid azure resource id."));
        verify(azurePrivateDnsZoneValidatorService, never()).existingPrivateDnsZoneNameIsSupported(any(), any(), eq(resultBuilder));
        verify(azurePrivateDnsZoneValidatorService, never()).privateDnsZoneExists(any(), any(), any());
        verify(azurePrivateDnsZoneValidatorService, never()).privateDnsZoneConnectedToNetwork(any(), anyString(), anyString(), any(), any());
    }

    @Test
    void testValidateWhenValidAndInvalidPrivateDnsZoneResourceId() {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        AzurePrivateDnsZoneDescriptor azurePrivateDnsZoneDescriptorA = new AzurePrivateDnsZoneDescriptorTestImpl("privateDnsZoneServiceA");
        AzurePrivateDnsZoneDescriptor azurePrivateDnsZoneDescriptorB = new AzurePrivateDnsZoneDescriptorTestImpl("privateDnsZoneServiceB");
        Map<AzurePrivateDnsZoneDescriptor, String> serviceToPrivateDnsZoneId = Map.of(
                azurePrivateDnsZoneDescriptorA, VALID_PRIVATE_DNS_ZONE_ID,
                azurePrivateDnsZoneDescriptorB, INVALID_PRIVATE_DNS_ZONE_ID
        );

        resultBuilder = underTest.validate(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME, serviceToPrivateDnsZoneId, resultBuilder);

        ValidationTestUtil.checkErrorsPresent(resultBuilder, List.of("The provided private DNS zone id invalidPrivateDnsZoneId for service " +
                "privateDnsZoneServiceB is not a valid azure resource id."));
        verify(azurePrivateDnsZoneValidatorService).existingPrivateDnsZoneNameIsSupported(eq(azurePrivateDnsZoneDescriptorA), any(),
                eq(resultBuilder));
        verify(azurePrivateDnsZoneValidatorService).privateDnsZoneExists(eq(azureClient), any(), eq(resultBuilder));
        verify(azurePrivateDnsZoneValidatorService).privateDnsZoneConnectedToNetwork(eq(azureClient), eq(NETWORK_RESOURCE_GROUP_NAME), eq(NETWORK_NAME),
                any(), eq(resultBuilder));

        verify(azurePrivateDnsZoneValidatorService, never()).existingPrivateDnsZoneNameIsSupported(eq(azurePrivateDnsZoneDescriptorB), any(), any());
    }

    private static class AzurePrivateDnsZoneDescriptorTestImpl implements AzurePrivateDnsZoneDescriptor {

        private final String resourceType;

        private AzurePrivateDnsZoneDescriptorTestImpl(String resourceType) {
            this.resourceType = resourceType;
        }

        @Override
        public String getResourceType() {
            return resourceType;
        }

        @Override
        public String getSubResource() {
            return "SubResourceTest";
        }

        @Override
        public String getDnsZoneName(String resourceGroupName) {
            return "ZoneNameTest";
        }

        @Override
        public List<Pattern> getDnsZoneNamePatterns() {
            return List.of(Pattern.compile("ZoneNamePatternTest"));
        }
    }

}

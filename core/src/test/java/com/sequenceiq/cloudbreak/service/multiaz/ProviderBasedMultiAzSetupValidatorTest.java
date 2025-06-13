package com.sequenceiq.cloudbreak.service.multiaz;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.AvailabilityZone;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;

@ExtendWith(MockitoExtension.class)
class ProviderBasedMultiAzSetupValidatorTest {

    private static final String INVALID_NUMBER_OF_ZONES_PATTERN = "The aRegion region does not support Multi AZ configuration. " +
            "Please check https://learn.microsoft.com/en-us/azure/reliability/availability-zones-service-support for more details. " +
            "It is also possible that the given %s instances on %s group are not supported in any specified [1, 2] zones.";

    private static final String NO_AVAILABILITY_ZONE_CONFIGURED_ON_ENV = "No availability zone configured on the environment, " +
            "multi/targeted availability zone could not be requested.";

    private static final String NO_SUBNET_PRESENT_FOR_INSTANCE_GROUP = "There were no SubnetIds defined for this Instance Group: is3.";

    private static final String STACK_MULTAZ_DISABLED_SOME_GROUPS_AZ_CONFIGURED = "The multi-AZ flag was not enabled, but zones were provided " +
            "on some of the groups of the deployment. Please use the multi-AZ flag or set explicit zone(s) for all the groups of the deployment!";

    private static final String NOT_ENTITLED_FOR_MULTIAZ_AZURE = String.format("Provisioning a multi AZ cluster on Azure requires entitlement %s.",
            Entitlement.CDP_CB_AZURE_MULTIAZ.name());

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private CredentialConverter credentialConverter;

    @Mock
    private StackService stackService;

    @Mock
    private ValidationResult.ValidationResultBuilder resultBuilder;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private ProviderBasedMultiAzSetupValidator underTest;

    @BeforeEach
    void setUp() {
        lenient().when(entitlementService.isAzureMultiAzEnabled(anyString())).thenReturn(Boolean.TRUE);
    }

    @Test
    void testValidateWhenAvailabilityZoneConnectorDoesNotExistForPlatform() {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudConnector.availabilityZoneConnector()).thenReturn(null);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        Stack stack = TestUtil.stack();
        stack.setMultiAz(Boolean.TRUE);

        underTest.validate(resultBuilder, stack);

        verifyNoInteractions(resultBuilder);
    }

    @Test
    void testValidateWhenAvailabilityZoneConnectorExistsForPlatformButStackHasNoGroups() {
        mockCredentialConversionFromEnvironmentWith(Set.of("1"));
        Stack stack = TestUtil.stack();
        stack.setInstanceGroups(Set.of());

        underTest.validate(resultBuilder, stack);

        verifyNoInteractions(resultBuilder);
    }

    @Test
    void testValidateWhenAwsStackDoNotHaveSubnetsForOneInstanceGroup() {
        Stack stack = TestUtil.stack();
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        stack.setMultiAz(Boolean.FALSE);
        stack.getInstanceGroups().forEach(ig -> {
            InstanceGroupNetwork ign = new InstanceGroupNetwork();
            ign.setAttributes(new Json("{\"subnetIds\": [\"subnet1\", \"subnet2\"]}"));
            if (!Objects.equals(ig.getGroupName(), "is3")) {
                ig.setInstanceGroupNetwork(ign);
            }
        });
        underTest.validate(resultBuilder, stack);
        verify(resultBuilder).error(NO_SUBNET_PRESENT_FOR_INSTANCE_GROUP);
    }

    @Test
    void testValidateWhenAwsStackHasDifferentSubnetsForDifferentInstanceGroupsAndEnableMultiAz() {
        Stack stack = TestUtil.stack();
        CloudConnector cloudConnector = mock(CloudConnector.class);
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        stack.setMultiAz(Boolean.FALSE);
        stack.getInstanceGroups().forEach(ig -> {
            if (Objects.equals(ig.getGroupName(), "is1")) {
                InstanceGroupNetwork ign = new InstanceGroupNetwork();
                ign.setAttributes(new Json("{\"subnetIds\": [\"subnet1\"]}"));
                ig.setInstanceGroupNetwork(ign);
            } else if (Objects.equals(ig.getGroupName(), "is2")) {
                InstanceGroupNetwork ign = new InstanceGroupNetwork();
                ign.setAttributes(new Json("{\"subnetIds\": [\"subnet2\"]}"));
                ig.setInstanceGroupNetwork(ign);
            } else if (Objects.equals(ig.getGroupName(), "is3")) {
                InstanceGroupNetwork ign = new InstanceGroupNetwork();
                ign.setAttributes(new Json("{\"subnetIds\": [\"subnet2\"]}"));
                ig.setInstanceGroupNetwork(ign);
            }
        });

        AvailabilityZoneConnector zoneConnector = mock(AvailabilityZoneConnector.class);
        when(cloudConnector.availabilityZoneConnector()).thenReturn(zoneConnector);
        when(zoneConnector.getAvailabilityZones(any(), any(), any(), any())).thenReturn(Set.of("az1", "az2"));
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setName("aRegion");

        CloudSubnet cloudSubnet1 = new CloudSubnet();
        cloudSubnet1.setAvailabilityZone("az1");
        cloudSubnet1.setId("subnet1");
        CloudSubnet cloudSubnet2 = new CloudSubnet();
        cloudSubnet2.setAvailabilityZone("az2");
        cloudSubnet2.setId("subnet2");
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setNetwork(EnvironmentNetworkResponse.builder()
                .withSubnetMetas(Map.of("subnet1", cloudSubnet1, "subnet2", cloudSubnet2))
                .build());
        detailedEnvironmentResponse.setCredential(new CredentialResponse());
        detailedEnvironmentResponse.setLocation(locationResponse);
        when(environmentClientService.getByCrn(any())).thenReturn(detailedEnvironmentResponse);
        when(credentialConverter.convert(any(CredentialResponse.class))).thenReturn(Credential.builder().build());
        when(extendedCloudCredentialConverter.convert(any()))
                .thenReturn(new ExtendedCloudCredential(new CloudCredential(), null, null, null, null));

        underTest.validate(resultBuilder, stack);

        verify(stackService).updateMultiAzFlag(stack.getId(), Boolean.TRUE);
        verifyNoInteractions(resultBuilder);
    }

    @Test
    void testValidateWhenAwsStackHasSameSubnetForAllInstanceGroups() {
        Stack stack = TestUtil.stack();
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        stack.setMultiAz(Boolean.FALSE);
        stack.getInstanceGroups().forEach(ig -> {
            InstanceGroupNetwork ign = new InstanceGroupNetwork();
            ign.setAttributes(new Json("{\"subnetIds\": [\"subnet1\"]}"));
            ig.setInstanceGroupNetwork(ign);
        });
        underTest.validate(resultBuilder, stack);

        verify(stackService, times(0)).updateMultiAzFlag(stack.getId(), Boolean.TRUE);
        verifyNoInteractions(resultBuilder);
    }

    @Test
    void testValidateWhenAwsStackHasMultipleSubnetsForInstanceGroupAndMultiAzDisabledForStack() {
        Stack stack = TestUtil.stack();
        CloudConnector cloudConnector = mock(CloudConnector.class);
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        stack.setMultiAz(Boolean.FALSE);
        stack.getInstanceGroups().forEach(ig -> {
            ig.setInstanceGroupType(InstanceGroupType.GATEWAY);
            InstanceGroupNetwork ign = new InstanceGroupNetwork();
            ign.setAttributes(new Json("{\"subnetIds\": [\"subnet1\", \"subnet2\"]}"));
            if (InstanceGroupType.GATEWAY.equals(ig.getInstanceGroupType())) {
                ig.setInstanceGroupNetwork(ign);
            }
        });

        AvailabilityZoneConnector zoneConnector = mock(AvailabilityZoneConnector.class);
        when(cloudConnector.availabilityZoneConnector()).thenReturn(zoneConnector);
        when(zoneConnector.getAvailabilityZones(any(), any(), any(), any())).thenReturn(Set.of("az1", "az2"));
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setName("aRegion");

        CloudSubnet cloudSubnet1 = new CloudSubnet();
        cloudSubnet1.setAvailabilityZone("az1");
        cloudSubnet1.setId("subnet1");
        CloudSubnet cloudSubnet2 = new CloudSubnet();
        cloudSubnet2.setAvailabilityZone("az2");
        cloudSubnet2.setId("subnet2");
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setNetwork(EnvironmentNetworkResponse.builder()
                .withSubnetMetas(Map.of("subnet1", cloudSubnet1, "subnet2", cloudSubnet2))
                .build());
        detailedEnvironmentResponse.setCredential(new CredentialResponse());
        detailedEnvironmentResponse.setLocation(locationResponse);
        when(environmentClientService.getByCrn(any())).thenReturn(detailedEnvironmentResponse);
        when(credentialConverter.convert(any(CredentialResponse.class))).thenReturn(Credential.builder().build());
        when(extendedCloudCredentialConverter.convert(any()))
                .thenReturn(new ExtendedCloudCredential(new CloudCredential(), null, null, null, null));

        underTest.validate(resultBuilder, stack);

        verify(stackService).updateMultiAzFlag(stack.getId(), Boolean.TRUE);
        verifyNoInteractions(resultBuilder);
    }

    @Test
    void testValidateWhenMultiAzRequestedForStackButNoZoneConfiguredOnEnvironment() {
        mockCredentialConversionFromEnvironmentWith(null);
        Stack stack = TestUtil.stack();
        stack.setInstanceGroups(Set.of());
        stack.setMultiAz(Boolean.TRUE);

        underTest.validate(resultBuilder, stack);

        verify(resultBuilder).error(NO_AVAILABILITY_ZONE_CONFIGURED_ON_ENV);
    }

    @Test
    void testValidateWhenMultiAzRequestedForStackConnectorExistForPlatformButNotEntitledForFeature() {
        AvailabilityZoneConnector zoneConnector = mock(AvailabilityZoneConnector.class);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudConnector.availabilityZoneConnector()).thenReturn(zoneConnector);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setInstanceGroups(Set.of());
        stack.setMultiAz(Boolean.TRUE);
        when(entitlementService.isAzureMultiAzEnabled(anyString())).thenReturn(Boolean.FALSE);

        underTest.validate(resultBuilder, stack);

        verify(resultBuilder).error(NOT_ENTITLED_FOR_MULTIAZ_AZURE);
    }

    @Test
    void testValidateWhenMultiAzRequestedForStackAndZonesConfiguredOnEnvironmentShouldSaveZonesToGroup() {
        Set<String> environmentZones = Set.of("1", "2");
        mockCredentialConversionFromEnvironmentWith(environmentZones);
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setMultiAz(Boolean.TRUE);

        underTest.validate(resultBuilder, stack);

        verify(instanceGroupService, times(stack.getInstanceGroups().size())).saveEnvironmentAvailabilityZones(any(), eq(environmentZones));
    }

    @Test
    void testValidateWhenMultiAzRequestedOnSomeOfTheGroupsButTheFlagOnStackIsDisabled() {
        Stack stack = TestUtil.stack();
        stack.getInstanceGroups().forEach(ig -> {
            AvailabilityZone availabilityZone = new AvailabilityZone();
            if (InstanceGroupType.CORE.equals(ig.getInstanceGroupType())) {
                availabilityZone.setAvailabilityZone("1");
                ig.setAvailabilityZones(Set.of(availabilityZone));
            }
        });
        stack.setMultiAz(Boolean.FALSE);

        underTest.validate(resultBuilder, stack);

        verify(resultBuilder).error(STACK_MULTAZ_DISABLED_SOME_GROUPS_AZ_CONFIGURED);
    }

    @Test
    void testValidateShouldAddViolationErrorWhenGroupLevelZonesAreLessThanTheRequiredMinimum() {
        AvailabilityZoneConnector zoneConnector = mockCredentialConversionFromEnvironmentWith(Set.of("1", "2"));
        when(zoneConnector.getAvailabilityZones(any(), any(), any(), any())).thenReturn(Set.of());
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setType(StackType.DATALAKE);
        stack.getInstanceGroups().forEach(ig -> {
            AvailabilityZone availabilityZone = new AvailabilityZone();
            availabilityZone.setAvailabilityZone("1");
            ig.setAvailabilityZones(Set.of(availabilityZone));
        });

        underTest.validate(resultBuilder, stack);

        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            verify(resultBuilder).error(String.format(INVALID_NUMBER_OF_ZONES_PATTERN,
                    instanceGroup.getTemplate().getInstanceType(), instanceGroup.getGroupName()));
        }
    }

    @Test
    void testValidateShouldAddViolationErrorWhenEnvironmentLevelZonesDoesNotContainGroupLevelZones() {
        String invalidZone = "1";
        Set<String> validZones = Set.of("3", "2");
        AvailabilityZoneConnector zoneConnector = mockCredentialConversionFromEnvironmentWith(validZones);
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setType(StackType.DATALAKE);
        stack.getInstanceGroups().forEach(ig -> {
            AvailabilityZone availabilityZone = new AvailabilityZone();
            availabilityZone.setAvailabilityZone(invalidZone);
            ig.setAvailabilityZones(Set.of(availabilityZone));
        });

        underTest.validate(resultBuilder, stack);

        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            verify(resultBuilder).error(String.format("These zones '%s' are requested for group '%s' but not available on Environment level('%s')",
                    invalidZone, instanceGroup.getGroupName(), String.join(",", validZones)));
        }
        verify(zoneConnector, times(0)).getAvailabilityZones(any(), any(), any(), any());
    }

    @Test
    void testValidateShouldNotNotSetMultiAzStackFlagWhenStackUpdateIsNotNecessaryBecauseGroupLevelZonesAreNotSet() {
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setType(StackType.WORKLOAD);
        stack.setId(1L);

        underTest.validate(resultBuilder, stack);

        verify(stackService, times(0)).updateMultiAzFlag(stack.getId(), Boolean.TRUE);
        verifyNoInteractions(resultBuilder);
    }

    @Test
    void testValidateShouldRSetStackLevelMultiAzFlagWhenAllGroupsHaveZonesAndMultiAzFlagIsFalseOnStackLevel() {
        Set<String> validZones = Set.of("1", "2");
        AvailabilityZoneConnector zoneConnector = mockCredentialConversionFromEnvironmentWith(validZones);
        when(zoneConnector.getAvailabilityZones(any(), any(), any(), any())).thenReturn(Set.of("1"));
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setType(StackType.WORKLOAD);
        stack.setId(1L);
        stack.getInstanceGroups().forEach(ig -> {
            AvailabilityZone availabilityZone = new AvailabilityZone();
            availabilityZone.setAvailabilityZone("2");
            AvailabilityZone otherAvailabilityZone = new AvailabilityZone();
            otherAvailabilityZone.setAvailabilityZone("1");
            ig.setAvailabilityZones(Set.of(availabilityZone, otherAvailabilityZone));
        });

        underTest.validate(resultBuilder, stack);

        verify(stackService, times(1)).updateMultiAzFlag(stack.getId(), Boolean.TRUE);
        verifyNoInteractions(resultBuilder);
    }

    private AvailabilityZoneConnector mockCredentialConversionFromEnvironmentWith(Set<String> availabilityZones) {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        AvailabilityZoneConnector zoneConnector = mock(AvailabilityZoneConnector.class);
        when(cloudConnector.availabilityZoneConnector()).thenReturn(zoneConnector);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setName("aRegion");
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        EnvironmentNetworkAzureParams environmentNetworkAzureParams = new EnvironmentNetworkAzureParams();
        environmentNetworkAzureParams.setAvailabilityZones(availabilityZones);
        environmentNetworkResponse.setAzure(environmentNetworkAzureParams);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCredential(new CredentialResponse());
        detailedEnvironmentResponse.setNetwork(environmentNetworkResponse);
        detailedEnvironmentResponse.setLocation(locationResponse);
        when(environmentClientService.getByCrn(any())).thenReturn(detailedEnvironmentResponse);
        when(credentialConverter.convert(any(CredentialResponse.class))).thenReturn(Credential.builder().build());
        when(extendedCloudCredentialConverter.convert(any()))
                .thenReturn(new ExtendedCloudCredential(new CloudCredential(), null, null, null, null));
        return zoneConnector;
    }
}
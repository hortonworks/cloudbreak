package com.sequenceiq.cloudbreak.controller.validation.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(MockitoExtension.class)
public class MultiAzValidatorTest {

    @InjectMocks
    private MultiAzValidator underTest;

    @Mock
    private ValidationResult.ValidationResultBuilder builder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "supportedMultiAzVariants", Set.of("AWS_NATIVE"));
        ReflectionTestUtils.setField(underTest, "supportedInstanceMetadataPlatforms", Set.of("AWS"));
        underTest.initSupportedVariants();
    }

    @Test
    public void testAwsNativeWhenOneSubnetUsedShouldNotCauseAnyValidationError() {
        String variant = "AWS_NATIVE";
        Set<InstanceGroup> instanceGroups = Set.of(
                instanceGroup(Set.of("subnet-123")),
                instanceGroup(Set.of("subnet-123"))
        );
        underTest.validateMultiAzForStack(variant, instanceGroups, builder);
        Mockito.verify(builder, Mockito.times(0)).error(anyString());
    }

    @Test
    public void testAwsNativeWhenMultipleSubnetUsedShouldNotCauseAnyValidationError() {
        String variant = "AWS_NATIVE";
        Set<InstanceGroup> instanceGroups = Set.of(
                instanceGroup(Set.of("subnet-123", "subnet-145")),
                instanceGroup(Set.of("subnet-123"))
        );
        underTest.validateMultiAzForStack(variant, instanceGroups, builder);
        Mockito.verify(builder, Mockito.times(0)).error(anyString());
    }

    @Test
    public void testAwsWhenOneSubnetUsedShouldNotCauseAnyValidationError() {
        String variant = "AWS";
        Set<InstanceGroup> instanceGroups = Set.of(
                instanceGroup(Set.of("subnet-123")),
                instanceGroup(Set.of("subnet-123"))
        );
        underTest.validateMultiAzForStack(variant, instanceGroups, builder);
        Mockito.verify(builder, Mockito.times(0)).error(anyString());
    }

    @Test
    public void testAwsWhenMultipleSubnetUsedShouldCauseValidationError() {
        String variant = "AWS";
        Set<InstanceGroup> instanceGroups = Set.of(
                instanceGroup(Set.of("subnet-123", "subnet-145")),
                instanceGroup(Set.of("subnet-123"))
        );
        underTest.validateMultiAzForStack(variant, instanceGroups, builder);
        Mockito.verify(builder, Mockito.times(1)).error(anyString());
    }

    @Test
    public void testSupportedForInstanceMetadataGenerationWhenPlatformSupportedShouldReturnTrue() {
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setCloudPlatform(CloudPlatform.AWS.name());
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);

        boolean supported = underTest.supportedForInstanceMetadataGeneration(instanceGroup);
        assertEquals(true, supported);
    }

    @Test
    public void testNotSupportedForInstanceMetadataGenerationWhenPlatformSupportedShouldReturnFalse() {
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setCloudPlatform(CloudPlatform.AZURE.name());
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);

        boolean supported = underTest.supportedForInstanceMetadataGeneration(instanceGroup);
        assertEquals(false, supported);
    }

    private InstanceGroup instanceGroup(Set<String> subnetIds) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork(subnetIds));
        return instanceGroup;
    }

    private InstanceGroupNetwork instanceGroupNetwork(Set<String> subnetIds) {
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        Map<String, Object> map = new HashMap<>();
        map.put(NetworkConstants.SUBNET_IDS, subnetIds);
        instanceGroupNetwork.setAttributes(new Json(map));
        return instanceGroupNetwork;
    }

}
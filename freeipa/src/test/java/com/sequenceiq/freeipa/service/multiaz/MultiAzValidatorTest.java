package com.sequenceiq.freeipa.service.multiaz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;

@ExtendWith(MockitoExtension.class)
public class MultiAzValidatorTest {

    @InjectMocks
    private MultiAzValidator underTest;

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
        assertDoesNotThrow(() -> underTest.validateMultiAzForStack(variant, instanceGroups));
    }

    @Test
    public void testAwsNativeWhenMultipleSubnetUsedShouldNotCauseAnyValidationError() {
        String variant = "AWS_NATIVE";
        Set<InstanceGroup> instanceGroups = Set.of(
                instanceGroup(Set.of("subnet-123", "subnet-145")),
                instanceGroup(Set.of("subnet-123"))
        );
        assertDoesNotThrow(() -> underTest.validateMultiAzForStack(variant, instanceGroups));
    }

    @Test
    public void testAwsWhenOneSubnetUsedShouldNotCauseAnyValidationError() {
        String variant = "AWS";
        Set<InstanceGroup> instanceGroups = Set.of(
                instanceGroup(Set.of("subnet-123")),
                instanceGroup(Set.of("subnet-123"))
        );
        assertDoesNotThrow(() -> underTest.validateMultiAzForStack(variant, instanceGroups));
    }

    @Test
    public void testAwsWhenMultipleSubnetUsedShouldCauseValidationError() {
        String variant = "AWS";
        Set<InstanceGroup> instanceGroups = Set.of(
                instanceGroup(Set.of("subnet-123", "subnet-145")),
                instanceGroup(Set.of("subnet-123"))
        );
        BadRequestException actual =
                assertThrows(BadRequestException.class, () -> underTest.validateMultiAzForStack(variant, instanceGroups));
        assertEquals("Multiple Availability Zone feature is not supported for AWS variant", actual.getMessage());
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
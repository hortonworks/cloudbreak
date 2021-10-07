package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AwsStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.GcpStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.YarnStackV4Parameters;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.distrox.api.v1.distrox.model.AwsDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.AzureDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.GcpDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.YarnDistroXV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;

class DistroXParameterConverterTest {

    private DistroXParameterConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new DistroXParameterConverter();
    }

    @Test
    void testAzureStackV4ParametersToAzureDistroXV1Parameters() {
        AzureStackV4Parameters input = new AzureStackV4Parameters();
        input.setEncryptStorage(true);
        input.setResourceGroupName("resourceGroupName");
        input.setLoadBalancerSku(LoadBalancerSku.STANDARD);

        AzureDistroXV1Parameters result = underTest.convert(input);

        assertNotNull(result);
        assertEquals(input.isEncryptStorage(), result.isEncryptStorage());
        assertEquals(input.getResourceGroupName(), result.getResourceGroupName());
        assertEquals(input.getLoadBalancerSku(), result.getLoadBalancerSku());
    }

    @Test
    void testAzureDistroXV1ParametersToAzureStackV4Parameters() {
        AzureDistroXV1Parameters input = new AzureDistroXV1Parameters();
        input.setEncryptStorage(true);
        input.setResourceGroupName("resourceGroupName");
        input.setLoadBalancerSku(LoadBalancerSku.STANDARD);

        AzureStackV4Parameters result = underTest.convert(input);

        assertNotNull(result);
        assertEquals(input.isEncryptStorage(), result.isEncryptStorage());
        assertEquals(input.getResourceGroupName(), result.getResourceGroupName());
        assertEquals(input.getLoadBalancerSku(), result.getLoadBalancerSku());
    }

    @Test
    void testAwsDistroXV1ParametersToAwsStackV4Parameters() {
        AwsDistroXV1Parameters input = new AwsDistroXV1Parameters();

        AwsStackV4Parameters result = underTest.convert(input);

        assertNotNull(result);
    }

    @Test
    void testAwsStackV4ParametersToAwsDistroXV1Parameters() {
        AwsStackV4Parameters input = new AwsStackV4Parameters();

        AwsDistroXV1Parameters result = underTest.convert(input);

        assertNotNull(result);
    }

    @Test
    void testGcpDistroXV1ParametersToGcpStackV4Parameters() {
        GcpDistroXV1Parameters input = new GcpDistroXV1Parameters();

        GcpStackV4Parameters result = underTest.convert(input);

        assertNotNull(result);
    }

    @Test
    void testGcpStackV4ParametersToGcpDistroXV1Parameters() {
        GcpStackV4Parameters input = new GcpStackV4Parameters();

        GcpDistroXV1Parameters result = underTest.convert(input);

        assertNotNull(result);
    }

    @Test
    void testYarnDistroXV1ParametersToYarnStackV4Parameters() {
        YarnDistroXV1Parameters input = new YarnDistroXV1Parameters();
        input.setYarnQueue("queue");
        input.setLifetime(10);

        YarnStackV4Parameters result = underTest.convert(input);

        assertNotNull(result);
        assertEquals(input.getLifetime(), result.getLifetime());
        assertEquals(input.getYarnQueue(), result.getYarnQueue());
    }

    @Test
    void testYarnStackV4ParametersToYarnDistroXV1Parameters() {
        YarnStackV4Parameters input = new YarnStackV4Parameters();
        input.setYarnQueue("queue");
        input.setLifetime(10);

        YarnDistroXV1Parameters result = underTest.convert(input);

        assertNotNull(result);
        assertEquals(input.getLifetime(), result.getLifetime());
        assertEquals(input.getYarnQueue(), result.getYarnQueue());
    }

    @Test
    void testEnvironmentNetworkYarnParamsToYarnStackV4Parameters() {
        EnvironmentNetworkYarnParams input = new EnvironmentNetworkYarnParams();
        input.setLifetime(10);
        input.setQueue("queue");

        YarnStackV4Parameters result = underTest.convert(input);

        assertNotNull(result);
        assertEquals(input.getLifetime(), result.getLifetime());
        assertEquals(input.getQueue(), result.getYarnQueue());
    }

}
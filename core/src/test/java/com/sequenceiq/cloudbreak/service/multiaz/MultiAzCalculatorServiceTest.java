package com.sequenceiq.cloudbreak.service.multiaz;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
public class MultiAzCalculatorServiceTest {

    @Mock
    private MultiAzValidator multiAzValidator;

    @InjectMocks
    private MultiAzCalculatorService underTest;

    @Test
    public void testPrepareSubnetAzMap() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetMetas(Map.of(
                cloudSubnetName(1), cloudSubnet(1),
                cloudSubnetName(2), cloudSubnet(2)
        ));
        detailedEnvironmentResponse.setNetwork(environmentNetworkResponse);

        Map<String, String> result = underTest.prepareSubnetAzMap(detailedEnvironmentResponse);
        Assertions.assertEquals(result.size(), 4);
        Assertions.assertEquals(result.get(cloudSubnetName(1)), cloudSubnetAz(1));
        Assertions.assertEquals(result.get(cloudSubnetName(2)), cloudSubnetAz(2));
    }

    static Object[][] testSubnetDistributionData() {
        return new Object[][]{
                // cloudPlatform, subnetCount, instanceCount, expectedCounts, supported
                { CloudPlatform.AWS,    1,  99,  Arrays.asList(99L),                  true   },
                { CloudPlatform.AWS,    3,  99,  Arrays.asList(33L, 33L, 33L),        true   },
                { CloudPlatform.AWS,    3,  60,  Arrays.asList(20L, 20L, 20L),        true   },
                { CloudPlatform.AWS,    3,  59,  Arrays.asList(20L, 20L, 19L),        true   },
                { CloudPlatform.AWS,    3,  58,  Arrays.asList(20L, 19L, 19L),        true   },
                { CloudPlatform.AWS,    3,  57,  Arrays.asList(19L, 19L, 19L),        true   },
                { CloudPlatform.AWS,    3,  56,  Arrays.asList(19L, 19L, 18L),        true   },
                { CloudPlatform.AWS,    4,  60,  Arrays.asList(15L, 15L, 15L, 15L),   true   },
                { CloudPlatform.AWS,    4,  59,  Arrays.asList(15L, 15L, 15L, 14L),   true   },
                { CloudPlatform.AWS,    4,  58,  Arrays.asList(15L, 15L, 14L, 14L),   true   },
                { CloudPlatform.AWS,    4,  57,  Arrays.asList(15L, 14L, 14L, 14L),   true   },
                { CloudPlatform.AWS,    4,  56,  Arrays.asList(14L, 14L, 14L, 14L),   true   },
                { CloudPlatform.YARN,   0,  99,  Arrays.asList(),                     false  },
        };
    }

    @ParameterizedTest(name = "testSubnetDistribution with {0} platform when {1} should result as {2}")
    @MethodSource("testSubnetDistributionData")
    public void testSubnetDistribution(CloudPlatform cloudPlatform, int subnetCount, int instanceCount,
        List<Long> expectedCounts, boolean supported) {
        if (supported) {
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroup.class))).thenReturn(true);
        }
        InstanceGroup instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup);

        List<Long> actualCounts = new ArrayList<>();
        for (int i = 0; i < subnetCount; i++) {
            int finalI = i;
            actualCounts.add(instanceGroup.getAllInstanceMetaData()
                    .stream()
                    .filter(e -> e.getSubnetId().equals(cloudSubnetName(finalI)))
                    .count());
        }
        Collections.sort(expectedCounts);
        Collections.sort(actualCounts);

        Assertions.assertEquals(expectedCounts, actualCounts);
    }

    private InstanceGroup instanceGroup(CloudPlatform cloudPlatform, int instanceNumber, int subnetCount) {
        InstanceGroup instanceGroup = new InstanceGroup();
        List<String> subnets = new ArrayList<>();
        for (int i = 0; i < subnetCount; i++) {
            instanceGroup.getAvailabilityZones().add(cloudSubnetAz(i));
            subnets.add(cloudSubnetName(i));
        }
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setCloudPlatform(cloudPlatform.name());
        instanceGroupNetwork.setAttributes(new Json(Map.of(NetworkConstants.SUBNET_IDS, subnets)));
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        for (int i = 0; i < instanceNumber; i++) {
            instanceGroup.getAllInstanceMetaData().add(instanceMetaData(i));
        }
        return instanceGroup;
    }

    private InstanceMetaData instanceMetaData(int i) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setId(Long.valueOf(i));
        return instanceMetaData;
    }

    private Map<String, String> subnetAzPairs(int maxNumber) {
        Map<String, String> subnetAzPairs = new HashMap<>();
        for (int i = 0; i < maxNumber; i++) {
            subnetAzPairs.put(cloudSubnetName(i), cloudSubnetAz(i));
        }
        return subnetAzPairs;
    }

    private CloudSubnet cloudSubnet(int i) {
        CloudSubnet cloudSubnet = new CloudSubnet();
        cloudSubnet.setId(String.valueOf(i));
        cloudSubnet.setName(cloudSubnetName(i));
        cloudSubnet.setAvailabilityZone(cloudSubnetAz(i));
        return cloudSubnet;
    }

    private String cloudSubnetName(int i) {
        return "name-" + i;
    }

    private String cloudSubnetAz(int i) {
        return "az-" + i;
    }
}
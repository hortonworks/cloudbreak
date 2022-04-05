package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@SpringBootTest(classes = InstanceMetadataServiceComponentTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ExtendWith(SpringExtension.class)
public class InstanceMetadataServiceComponentTest {

    private static final String ENV_CRN = "envCrn";

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Test
    public void saveInstanceAndGetUpdatedStack() {

        DetailedEnvironmentResponse detailedEnvResponse = DetailedEnvironmentResponse.builder()
                .withCrn(ENV_CRN)
                .withNetwork(EnvironmentNetworkResponse.builder()
                        .withSubnetMetas(Map.of(
                                "sub1", cloudSubnet("az", "sub1"),
                                "sub2", cloudSubnet("az", "sub2"),
                                "sub3", cloudSubnet("az", "sub3"),
                                "sub4", cloudSubnet("az1", "sub4")
                        ))
                        .build())
                .build();
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENV_CRN);
        InstanceGroup workerInstanceGroup = new InstanceGroup();
        workerInstanceGroup.setGroupName("worker");
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setCloudPlatform("AWS");
        instanceGroupNetwork.setAttributes(new Json(Map.of(SUBNET_IDS, List.of("sub1", "sub2", "sub3", "sub4"))));
        workerInstanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        stack.setInstanceGroups(Set.of(workerInstanceGroup));
        when(environmentClientService.getByCrn(ENV_CRN)).thenReturn(detailedEnvResponse);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, Map.of("worker", 42), Map.of(), false, false, null);
        Map<String, List<InstanceMetaData>> groupBySub = workerInstanceGroup.getInstanceMetaDataSet().stream()
                .collect(Collectors.groupingBy(
                        InstanceMetaData::getSubnetId,
                        Collectors.mapping(Function.identity(), Collectors.toList())));
        Map<String, List<InstanceMetaData>> groupByAz = workerInstanceGroup.getInstanceMetaDataSet().stream()
                .collect(Collectors.groupingBy(
                        InstanceMetaData::getAvailabilityZone,
                        Collectors.mapping(Function.identity(), Collectors.toList())));

        Assertions.assertEquals(2, groupByAz.size());
        Assertions.assertEquals(21, groupByAz.get("az").size());
        Assertions.assertEquals(21, groupByAz.get("az1").size());
        Assertions.assertEquals(4, groupBySub.size());
        Assertions.assertEquals(7, groupBySub.get("sub1").size());
        Assertions.assertEquals(7, groupBySub.get("sub2").size());
        Assertions.assertEquals(7, groupBySub.get("sub3").size());
        Assertions.assertEquals(21, groupBySub.get("sub4").size());

    }

    @Test
    public void saveInstanceAndGetUpdatedStackWhenPreferredSubnetSet() {

        DetailedEnvironmentResponse detailedEnvResponse = DetailedEnvironmentResponse.builder()
                .withCrn(ENV_CRN)
                .withNetwork(EnvironmentNetworkResponse.builder()
                        .withSubnetMetas(Map.of(
                                "sub1", cloudSubnet("az", "sub1"),
                                "sub2", cloudSubnet("az", "sub2"),
                                "sub3", cloudSubnet("az", "sub3"),
                                "sub4", cloudSubnet("az1", "sub4")
                        ))
                        .build())
                .build();
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENV_CRN);
        InstanceGroup workerInstanceGroup = new InstanceGroup();
        workerInstanceGroup.setGroupName("worker");
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setCloudPlatform("AWS");
        instanceGroupNetwork.setAttributes(new Json(Map.of(SUBNET_IDS, List.of("sub1", "sub2", "sub3", "sub4"))));
        workerInstanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        stack.setInstanceGroups(Set.of(workerInstanceGroup));
        when(environmentClientService.getByCrn(ENV_CRN)).thenReturn(detailedEnvResponse);
        NetworkScaleDetails networkScaleDetails = new NetworkScaleDetails(List.of("sub1", "sub2"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, Map.of("worker", 42), Map.of(), false, false, networkScaleDetails);

        Map<String, List<InstanceMetaData>> groupBySub = workerInstanceGroup.getInstanceMetaDataSet().stream()
                .collect(Collectors.groupingBy(
                        InstanceMetaData::getSubnetId,
                        Collectors.mapping(Function.identity(), Collectors.toList())));
        Map<String, List<InstanceMetaData>> groupByAz = workerInstanceGroup.getInstanceMetaDataSet().stream()
                .collect(Collectors.groupingBy(
                        InstanceMetaData::getAvailabilityZone,
                        Collectors.mapping(Function.identity(), Collectors.toList())));

        Assertions.assertEquals(1, groupByAz.size());
        Assertions.assertEquals(42, groupByAz.get("az").size());
        Assertions.assertNull(groupByAz.get("az1"));
        Assertions.assertEquals(2, groupBySub.size());
        Assertions.assertEquals(21, groupBySub.get("sub1").size());
        Assertions.assertEquals(21, groupBySub.get("sub2").size());
        Assertions.assertNull(groupBySub.get("sub3"));
        Assertions.assertNull(groupBySub.get("sub4"));

    }

    private CloudSubnet cloudSubnet(String az, String sub) {
        CloudSubnet cloudSubnet = new CloudSubnet();
        cloudSubnet.setAvailabilityZone(az);
        cloudSubnet.setId(sub);
        return cloudSubnet;
    }

    @ComponentScan(
            basePackages = {
                    "com.sequenceiq.cloudbreak.service.stack",
                    "com.sequenceiq.cloudbreak.service.multiaz",
                    "com.sequenceiq.cloudbreak.controller.validation.network"},
            excludeFilters = {@ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = "(?!.*\\.InstanceMetaDataService)com\\.sequenceiq\\.cloudbreak\\.service\\.stack\\..*")}
    )
    public static class TestConfig {

        @MockBean
        private InstanceMetaDataRepository repository;

        @MockBean
        private TransactionService transactionService;

        @MockBean
        private EnvironmentClientService environmentClientService;

        @MockBean
        private ResourceRetriever resourceRetriever;

        @MockBean
        private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;
    }
}

package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@SpringBootTest(classes = InstanceMetadataServiceComponentTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ExtendWith(SpringExtension.class)
public class InstanceMetadataServiceComponentTest {

    private static final String ENV_CRN = "envCrn";

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Inject
    private FreeIpaService freeIpaService;

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
        when(cachedEnvironmentClientService.getByCrn(ENV_CRN)).thenReturn(detailedEnvResponse);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setHostname("hostname");
        freeIpa.setDomain("domain");
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);

        instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, cloudInstances(42));
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

    private List<CloudInstance> cloudInstances(int count) {
        List<CloudInstance> instances = new ArrayList<>();
        for (long i = 0; i < count; i++) {
            InstanceTemplate instanceTemplate = new InstanceTemplate("flavor", "worker", i, Collections.emptyList(), InstanceStatus.CREATED,
                    Collections.emptyMap(), i, "iid", null, 0L);
            instances.add(new CloudInstance("i-" + i, instanceTemplate, null, null, null));
        }
        return instances;
    }

    private CloudSubnet cloudSubnet(String az, String sub) {
        CloudSubnet cloudSubnet = new CloudSubnet();
        cloudSubnet.setAvailabilityZone(az);
        cloudSubnet.setId(sub);
        return cloudSubnet;
    }

    @ComponentScan(
            basePackages = {
                    "com.sequenceiq.freeipa.service.stack",
                    "com.sequenceiq.freeipa.service.multiaz"},
            excludeFilters = {@ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = "(?!.*\\.InstanceMetaDataService)com\\.sequenceiq\\.freeipa\\.service\\.stack\\..*")}
    )
    public static class TestConfig {

        @MockBean
        private InstanceMetaDataRepository repository;

        @MockBean
        private TransactionService transactionService;

        @MockBean
        private CachedEnvironmentClientService cachedEnvironmentClientService;

        @MockBean
        private FreeIpaService freeIpaService;
    }
}

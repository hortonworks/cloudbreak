package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.AvailabilityZoneConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.image.ImageService;
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

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Captor
    private ArgumentCaptor<InstanceMetaData> instanceMetaDataArgumentCaptor;

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

        instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, cloudInstances(42, "worker"), Collections.emptyList());
        Map<String, List<InstanceMetaData>> groupBySub = workerInstanceGroup.getInstanceMetaDataSet().stream()
                .collect(Collectors.groupingBy(
                        InstanceMetaData::getSubnetId,
                        Collectors.mapping(Function.identity(), Collectors.toList())));
        Map<String, List<InstanceMetaData>> groupByAz = workerInstanceGroup.getInstanceMetaDataSet().stream()
                .collect(Collectors.groupingBy(
                        InstanceMetaData::getAvailabilityZone,
                        Collectors.mapping(Function.identity(), Collectors.toList())));

        assertEquals(2, groupByAz.size());
        assertEquals(21, groupByAz.get("az").size());
        assertEquals(21, groupByAz.get("az1").size());
        assertEquals(4, groupBySub.size());
        assertEquals(7, groupBySub.get("sub1").size());
        assertEquals(7, groupBySub.get("sub2").size());
        assertEquals(7, groupBySub.get("sub3").size());
        assertEquals(21, groupBySub.get("sub4").size());
        verify(instanceMetaDataRepository, times(42)).save(instanceMetaDataArgumentCaptor.capture());
        assertTrue(instanceMetaDataArgumentCaptor.getAllValues()
                .stream()
                .allMatch(im -> StringUtils.isNotEmpty(im.getSubnetId())));
    }

    @Test
    public void saveInstanceAndGetUpdatedStackForMasterRepairOfOneNodeFromTheTwo() {

        DetailedEnvironmentResponse detailedEnvResponse = DetailedEnvironmentResponse.builder()
                .withCrn(ENV_CRN)
                .withNetwork(EnvironmentNetworkResponse.builder()
                        .withSubnetMetas(Map.of(
                                "subnet-06f0", cloudSubnet("us-gov-west-1b", "subnet-06f0"),
                                "subnet-0c49", cloudSubnet("us-gov-west-1c", "subnet-0c49"),
                                "subnet-0a48", cloudSubnet("us-gov-west-1a", "subnet-0a48")
                        ))
                        .build())
                .build();
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENV_CRN);
        InstanceGroup masterInstanceGroup = new InstanceGroup();
        masterInstanceGroup.setGroupName("master");
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setCloudPlatform("AWS");
        instanceGroupNetwork.setAttributes(new Json(Map.of(SUBNET_IDS, List.of("subnet-0c49", "subnet-06f0"))));
        masterInstanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        InstanceMetaData masterInstance = new InstanceMetaData();
        masterInstance.setPrivateId(3000L);
        masterInstance.setInstanceId("i-masterinstance");
        masterInstance.setAvailabilityZone("us-gov-west-1c");
        masterInstance.setSubnetId("subnet-0c49");
        masterInstanceGroup.getInstanceMetaData().add(masterInstance);
        stack.setInstanceGroups(Set.of(masterInstanceGroup));
        when(cachedEnvironmentClientService.getByCrn(ENV_CRN)).thenReturn(detailedEnvResponse);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setHostname("hostname");
        freeIpa.setDomain("domain");
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);

        instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, cloudInstances(1, "master"), Collections.emptyList());
        Map<String, List<InstanceMetaData>> groupBySub = masterInstanceGroup.getInstanceMetaDataSet().stream()
                .collect(Collectors.groupingBy(
                        InstanceMetaData::getSubnetId,
                        Collectors.mapping(Function.identity(), Collectors.toList())));
        Map<String, List<InstanceMetaData>> groupByAz = masterInstanceGroup.getInstanceMetaDataSet().stream()
                .collect(Collectors.groupingBy(
                        InstanceMetaData::getAvailabilityZone,
                        Collectors.mapping(Function.identity(), Collectors.toList())));

        assertEquals(2, groupByAz.size());
        assertEquals(2, groupBySub.size());
        verify(instanceMetaDataRepository, times(1)).save(instanceMetaDataArgumentCaptor.capture());
        assertTrue(instanceMetaDataArgumentCaptor.getAllValues()
                .stream()
                .allMatch(im -> StringUtils.isNotEmpty(im.getSubnetId())));
    }

    private List<CloudInstance> cloudInstances(int count, String groupName) {
        List<CloudInstance> instances = new ArrayList<>();
        for (long i = 0; i < count; i++) {
            InstanceTemplate instanceTemplate = new InstanceTemplate("flavor", groupName, i, Collections.emptyList(), InstanceStatus.CREATED,
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

        @MockBean
        private CloudPlatformConnectors cloudPlatformConnectors;

        @MockBean
        private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

        @MockBean
        private CredentialService credentialService;

        @MockBean
        private ImageService imageService;

        @MockBean
        private AvailabilityZoneConverter availabilityZoneConverter;

        @MockBean
        private Clock clock;
    }
}

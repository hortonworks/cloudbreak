package com.sequenceiq.cloudbreak.service.cost;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.co2.model.ClusterCO2Dto;
import com.sequenceiq.cloudbreak.co2.model.DiskCO2Dto;
import com.sequenceiq.cloudbreak.co2.model.InstanceGroupCO2Dto;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.cost.cloudera.ClouderaCostCache;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.DiskCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
public class InstanceTypeCollectorServiceTest {

    private static final String REGION = "REGION";

    private static final String INSTANCE_TYPE = "INSTANCE_TYPE";

    private static final double MAGIC_PRICE_PER_DISK_GB = 0.000138;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private Map<CloudPlatform, PricingCache> pricingCaches;

    @Mock
    private PricingCache pricingCache;

    @Mock
    private ClouderaCostCache clouderaCostCache;

    @InjectMocks
    private InstanceTypeCollectorService underTest;

    @Test
    void getAllInstanceTypesForCost() {
        when(instanceMetaDataService.countByInstanceGroupId(420L)).thenReturn(2);
        when(clouderaCostCache.getPriceByType(any())).thenReturn(0.5);
        when(pricingCaches.containsKey(any(CloudPlatform.class))).thenReturn(Boolean.TRUE);
        when(pricingCaches.get(any(CloudPlatform.class))).thenReturn(pricingCache);
        when(pricingCache.getPriceForInstanceType(REGION, INSTANCE_TYPE)).thenReturn(0.5);
        when(pricingCache.getStoragePricePerGBHour(eq(REGION), any(), anyInt())).thenReturn(MAGIC_PRICE_PER_DISK_GB);
        when(instanceGroupService.getInstanceGroupViewByStackId(69L)).thenReturn(List.of(getInstanceGroup("gp2")));

        ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:1", () -> {
            ClusterCostDto clusterCostDto = underTest.getAllInstanceTypesForCost(getStack("AZURE"));

            Assertions.assertEquals("AVAILABLE", clusterCostDto.getStatus());
            Assertions.assertEquals(REGION.toLowerCase(), clusterCostDto.getRegion());
            Optional<InstanceGroupCostDto> instanceGroupCostDtoOptional = clusterCostDto.getInstanceGroups().stream().findFirst();
            Assertions.assertTrue(instanceGroupCostDtoOptional.isPresent());
            InstanceGroupCostDto instanceGroupCostDto = instanceGroupCostDtoOptional.get();
            Assertions.assertEquals(1.0, instanceGroupCostDto.getTotalProviderPrice());
            Assertions.assertEquals(1.0, instanceGroupCostDto.getTotalClouderaPrice());
            Optional<DiskCostDto> diskCostDtoOptional = instanceGroupCostDto.getDisksPerInstance().stream().findFirst();
            Assertions.assertTrue(diskCostDtoOptional.isPresent());
            DiskCostDto diskCostDto = diskCostDtoOptional.get();
            Assertions.assertEquals(500, diskCostDto.getTotalDiskSizeInGb());
            Assertions.assertEquals(MAGIC_PRICE_PER_DISK_GB * 2 * 250, diskCostDto.getTotalDiskPrice());
        });
    }

    @Test
    void getAllInstanceTypesForCO2() {
        when(instanceMetaDataService.countByInstanceGroupId(420L)).thenReturn(2);
        when(pricingCaches.containsKey(any(CloudPlatform.class))).thenReturn(Boolean.TRUE);
        when(pricingCaches.get(any(CloudPlatform.class))).thenReturn(pricingCache);
        when(pricingCache.getCpuCountForInstanceType(eq(REGION), eq(INSTANCE_TYPE), any())).thenReturn(8);
        when(pricingCache.getMemoryForInstanceType(eq(REGION), eq(INSTANCE_TYPE), any())).thenReturn(16);
        when(instanceGroupService.getInstanceGroupViewByStackId(69L)).thenReturn(List.of(getInstanceGroup("gp2")));
        when(credentialClientService.getByEnvironmentCrn(any())).thenReturn(getCredential("AWS"));

        ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:1", () -> {
            ClusterCO2Dto clusterCO2Dto = underTest.getAllInstanceTypesForCO2(getStack("AWS"));

            Assertions.assertEquals("AVAILABLE", clusterCO2Dto.getStatus());
            Assertions.assertEquals(REGION, clusterCO2Dto.getRegion());
            Optional<InstanceGroupCO2Dto> instanceGroupCO2DtoOptional = clusterCO2Dto.getInstanceGroups().stream().findFirst();
            Assertions.assertTrue(instanceGroupCO2DtoOptional.isPresent());
            InstanceGroupCO2Dto instanceGroupCO2Dto = instanceGroupCO2DtoOptional.get();
            Assertions.assertEquals(8, instanceGroupCO2Dto.getvCPUs());
            Assertions.assertEquals(16, instanceGroupCO2Dto.getMemory());
            Optional<DiskCO2Dto> diskCO2DtoOptional = instanceGroupCO2Dto.getDisksPerInstance().stream().findFirst();
            Assertions.assertTrue(diskCO2DtoOptional.isPresent());
            DiskCO2Dto diskCO2Dto = diskCO2DtoOptional.get();
            Assertions.assertEquals(2, diskCO2Dto.getCount());
            Assertions.assertEquals(250, diskCO2Dto.getSize());
        });
    }

    private Stack getStack(String cloudPlatform) {
        Stack stack = new Stack();
        stack.setId(69L);
        stack.setRegion(REGION);
        stack.setCloudPlatform(cloudPlatform);
        stack.setStackStatus(new StackStatus(stack, Status.AVAILABLE, "Status reason.", DetailedStackStatus.AVAILABLE));
        stack.setInstanceGroups(Set.of(getInstanceGroup("standard")));
        return stack;
    }

    private InstanceGroup getInstanceGroup(String volumeType) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(420L);
        Template template = new Template();
        template.setInstanceType(INSTANCE_TYPE);
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeCount(2);
        volumeTemplate.setVolumeSize(250);
        volumeTemplate.setVolumeType(volumeType);
        template.setVolumeTemplates(Set.of(volumeTemplate));
        instanceGroup.setTemplate(template);
        instanceGroup.setInstanceMetaData(Set.of(new InstanceMetaData(), new InstanceMetaData()));
        return instanceGroup;
    }

    private Credential getCredential(String cloudPlatform) {
        return Credential.builder()
                .crn("CRN")
                .name("NAME")
                .account("ACCOUNT")
                .cloudPlatform(cloudPlatform)
                .attributes(new Json(Map.of(cloudPlatform, Map.of("TEST", "OBJECT"))))
                .build();
    }
}

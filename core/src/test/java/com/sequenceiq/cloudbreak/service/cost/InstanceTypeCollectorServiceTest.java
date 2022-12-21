package com.sequenceiq.cloudbreak.service.cost;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.AwsPricingCache;
import com.sequenceiq.cloudbreak.cloud.azure.cost.AzurePricingCache;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.cost.cloudera.ClouderaCostCache;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.DiskCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
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
    private AwsPricingCache awsPricingCache;

    @Mock
    private AzurePricingCache azurePricingCache;

    @Mock
    private ClouderaCostCache clouderaCostCache;

    @InjectMocks
    private InstanceTypeCollectorService underTest;

    @BeforeEach
    void setup() {
        when(instanceMetaDataService.countByInstanceGroupId(420L)).thenReturn(2);
        when(clouderaCostCache.getPriceByType(any())).thenReturn(0.5);
    }

    @Test
    void getAllInstanceTypesAWS() {
        when(awsPricingCache.getPriceForInstanceType(REGION, INSTANCE_TYPE)).thenReturn(0.5);
        when(awsPricingCache.getCpuCountForInstanceType(REGION, INSTANCE_TYPE)).thenReturn(8);
        when(awsPricingCache.getMemoryForInstanceType(REGION, INSTANCE_TYPE)).thenReturn(16);
        when(awsPricingCache.getStoragePricePerGBHour(REGION, "gp2")).thenReturn(MAGIC_PRICE_PER_DISK_GB);
        when(instanceGroupService.getInstanceGroupViewByStackId(69L)).thenReturn(List.of(getInstanceGroup("gp2")));
        when(credentialClientService.getByEnvironmentCrn(any())).thenReturn(getCredential("AWS"));

        ClusterCostDto clusterCostDto = underTest.getAllInstanceTypes(getStack("AWS"));

        assertions(clusterCostDto);
    }

    @Test
    void getAllInstanceTypesAzure() {
        when(azurePricingCache.getPriceForInstanceType(REGION, INSTANCE_TYPE)).thenReturn(0.5);
        when(azurePricingCache.getCpuCountForInstanceType(eq(REGION), eq(INSTANCE_TYPE), any())).thenReturn(8);
        when(azurePricingCache.getMemoryForInstanceType(eq(REGION), eq(INSTANCE_TYPE), any())).thenReturn(16);
        when(azurePricingCache.getStoragePricePerGBHour(REGION, "StandardSSD_LRS", 250)).thenReturn(MAGIC_PRICE_PER_DISK_GB);
        when(instanceGroupService.getInstanceGroupViewByStackId(69L)).thenReturn(List.of(getInstanceGroup("StandardSSD_LRS")));
        when(credentialClientService.getByEnvironmentCrn(any())).thenReturn(getCredential("AZURE"));

        ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:1", () -> {
            ClusterCostDto clusterCostDto = underTest.getAllInstanceTypes(getStack("AZURE"));

            assertions(clusterCostDto);
        });
    }

    private void assertions(ClusterCostDto clusterCostDto) {
        Assertions.assertEquals("AVAILABLE", clusterCostDto.getStatus());
        Assertions.assertEquals(REGION.toLowerCase(), clusterCostDto.getRegion());
        InstanceGroupCostDto instanceGroupCostDto = clusterCostDto.getInstanceGroups().get(0);
        Assertions.assertEquals(1.0, instanceGroupCostDto.getTotalProviderPrice());
        Assertions.assertEquals(8, instanceGroupCostDto.getCoresPerInstance());
        Assertions.assertEquals(16, instanceGroupCostDto.getMemoryPerInstance());
        Assertions.assertEquals(1.0, instanceGroupCostDto.getTotalClouderaPrice());
        DiskCostDto diskCostDto = instanceGroupCostDto.getDisksPerInstance().get(0);
        Assertions.assertEquals(500, diskCostDto.getTotalDiskSizeInGb());
        Assertions.assertEquals(MAGIC_PRICE_PER_DISK_GB * 2 * 250, diskCostDto.getTotalDiskPrice());
    }

    private Stack getStack(String cloudPlatform) {
        Stack stack = new Stack();
        stack.setId(69L);
        stack.setRegion(REGION);
        stack.setCloudPlatform(cloudPlatform);
        stack.setStackStatus(new StackStatus(stack, Status.AVAILABLE, "Status reason.", DetailedStackStatus.AVAILABLE));
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

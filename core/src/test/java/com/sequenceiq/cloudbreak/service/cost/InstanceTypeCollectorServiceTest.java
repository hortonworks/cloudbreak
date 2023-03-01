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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.PricingCache;
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

    @BeforeEach
    void setup() {
        when(instanceMetaDataService.countByInstanceGroupId(420L)).thenReturn(2);
        when(clouderaCostCache.getPriceByType(any())).thenReturn(0.5);
        when(pricingCaches.containsKey(any())).thenReturn(Boolean.TRUE);
        when(pricingCaches.get(any())).thenReturn(pricingCache);
    }

    @Test
    void getAllInstanceTypes() {
        when(pricingCache.getPriceForInstanceType(eq(REGION), eq(INSTANCE_TYPE), any())).thenReturn(Optional.of(0.5));
        when(pricingCache.getCpuCountForInstanceType(eq(REGION), eq(INSTANCE_TYPE), any())).thenReturn(Optional.of(8));
        when(pricingCache.getMemoryForInstanceType(eq(REGION), eq(INSTANCE_TYPE), any())).thenReturn(Optional.of(16));
        when(pricingCache.getStoragePricePerGBHour(eq(REGION), any(), anyInt())).thenReturn(Optional.of(MAGIC_PRICE_PER_DISK_GB));
        when(instanceGroupService.getInstanceGroupViewByStackId(69L)).thenReturn(List.of(getInstanceGroup("gp2")));
        when(credentialClientService.getByEnvironmentCrn(any())).thenReturn(getCredential("AZURE"));

        ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:1", () -> {
            Optional<ClusterCostDto> clusterCostDto = underTest.getAllInstanceTypes(getStack("AZURE"));

            Assertions.assertTrue(clusterCostDto.isPresent());
            assertions(clusterCostDto.get());
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

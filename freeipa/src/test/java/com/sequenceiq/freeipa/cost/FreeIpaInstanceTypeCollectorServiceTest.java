package com.sequenceiq.freeipa.cost;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.AwsPricingCache;
import com.sequenceiq.cloudbreak.cloud.azure.cost.AzurePricingCache;
import com.sequenceiq.cloudbreak.cost.cloudera.ClouderaCostCache;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.DiskCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.CredentialService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaInstanceTypeCollectorServiceTest {

    private static final String REGION = "REGION";

    private static final String INSTANCE_TYPE = "INSTANCE_TYPE";

    private static final double MAGIC_PRICE_PER_DISK_GB = 0.000138;

    @Mock
    private CredentialService credentialService;

    @Mock
    private AwsPricingCache awsPricingCache;

    @Mock
    private AzurePricingCache azurePricingCache;

    @Mock
    private ClouderaCostCache clouderaCostCache;

    @InjectMocks
    private FreeIpaInstanceTypeCollectorService underTest;

    @BeforeEach
    void setup() {
        when(clouderaCostCache.getPriceByType(any())).thenReturn(0.5);
    }

    @Test
    void getAllInstanceTypesAWS() {
        when(awsPricingCache.getPriceForInstanceType(REGION, INSTANCE_TYPE)).thenReturn(0.5);
        when(awsPricingCache.getCpuCountForInstanceType(REGION, INSTANCE_TYPE)).thenReturn(8);
        when(awsPricingCache.getMemoryForInstanceType(REGION, INSTANCE_TYPE)).thenReturn(16);
        when(credentialService.getCredentialByEnvCrn(any())).thenReturn(getCredential("AWS"));

        ClusterCostDto clusterCostDto = underTest.getAllInstanceTypes(getStack("AWS"));

        assertions(clusterCostDto);
    }

    @Test
    void getAllInstanceTypesAzure() {
        when(azurePricingCache.getPriceForInstanceType(REGION, INSTANCE_TYPE)).thenReturn(0.5);
        when(azurePricingCache.getCpuCountForInstanceType(eq(REGION), eq(INSTANCE_TYPE), any())).thenReturn(8);
        when(azurePricingCache.getMemoryForInstanceType(eq(REGION), eq(INSTANCE_TYPE), any())).thenReturn(16);
        when(credentialService.getCredentialByEnvCrn(any())).thenReturn(getCredential("AZURE"));

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
        stack.setEnvironmentCrn("ENVIRONMENT_CRN");
        stack.setRegion(REGION);
        stack.setCloudPlatform(cloudPlatform);
        stack.setStackStatus(new StackStatus(stack, Status.AVAILABLE, "Status reason.", DetailedStackStatus.AVAILABLE));
        stack.setInstanceGroups(Set.of(getInstanceGroup()));
        return stack;
    }

    private InstanceGroup getInstanceGroup() {
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        template.setInstanceType(INSTANCE_TYPE);
        template.setVolumeCount(2);
        template.setRootVolumeSize(250);
        instanceGroup.setTemplate(template);
        instanceGroup.setInstanceMetaData(Set.of(new InstanceMetaData(), new InstanceMetaData()));
        return instanceGroup;
    }

    private Credential getCredential(String cloudPlatform) {
        return new Credential(cloudPlatform, "NAME", "{\"AZURE\":{\"TEST\":\"OBJECT\"}}", "CRN", "ACCOUNT");
    }
}

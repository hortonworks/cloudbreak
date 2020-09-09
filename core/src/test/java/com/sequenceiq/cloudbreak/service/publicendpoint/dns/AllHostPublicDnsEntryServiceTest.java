package com.sequenceiq.cloudbreak.service.publicendpoint.dns;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@ExtendWith(MockitoExtension.class)
class AllHostPublicDnsEntryServiceTest {

    @InjectMocks
    private AllHostPublicDnsEntryService underTest;

    @Test
    void getComponentLocationWhenPrimaryGatewayIsTheOnlyNodeWithinGroup() {
        Stack stack = TestUtil.stack();

        Map<String, List<String>> result = underTest.getComponentLocation(stack);

        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        Assertions.assertFalse(result.containsKey(primaryGatewayInstance.getInstanceGroupName()), "Result should not contain primary gateway's group name");
        Assertions.assertFalse(resultContainsInstanceMetadata(primaryGatewayInstance, result), "Result should not contain primary gateway's instance metadata");
    }

    @Test
    void getComponentLocationWhenPrimaryGatewayIsNotTheOnlyNodeWithinGatewayHostGroup() {
        Stack stack = TestUtil.stack();
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        InstanceGroup gatewayInstanceGroup = primaryGatewayInstance.getInstanceGroup();
        InstanceMetaData otherGatewayInstanceMetadata = new InstanceMetaData();
        otherGatewayInstanceMetadata.setDiscoveryFQDN("something.new");
        otherGatewayInstanceMetadata.setInstanceGroup(gatewayInstanceGroup);
        otherGatewayInstanceMetadata.setInstanceStatus(InstanceStatus.SERVICES_RUNNING);
        Set<InstanceMetaData> updatedIM = gatewayInstanceGroup.getNotTerminatedInstanceMetaDataSet();
        updatedIM.add(otherGatewayInstanceMetadata);
        gatewayInstanceGroup.replaceInstanceMetadata(updatedIM);


        Map<String, List<String>> result = underTest.getComponentLocation(stack);

        Assertions.assertTrue(result.containsKey(primaryGatewayInstance.getInstanceGroupName()), "Result should contain primary gateway's group name");
        Assertions.assertTrue(resultContainsInstanceMetadata(otherGatewayInstanceMetadata, result), "Result should contain other gateway's instance metadata");
        Assertions.assertFalse(resultContainsInstanceMetadata(primaryGatewayInstance, result), "Result should not contain primary gateway's instance metadata");
    }

    private boolean resultContainsInstanceMetadata(InstanceMetaData primaryGatewayInstance, Map<String, List<String>> result) {
        return result
                .values()
                .stream()
                .anyMatch(ims -> ims.stream().anyMatch(im -> im.equals(primaryGatewayInstance.getDiscoveryFQDN())));
    }
}
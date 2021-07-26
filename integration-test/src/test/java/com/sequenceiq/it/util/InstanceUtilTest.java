package com.sequenceiq.it.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.util.InstanceUtil;

@ExtendWith(MockitoExtension.class)
public class InstanceUtilTest {

    @Test
    public void testGetInstanceStatusMap() {
        assertEquals(0, InstanceUtil.getInstanceStatusMap(
                getResponse()).entrySet().size());
        assertEquals(0, InstanceUtil.getInstanceStatusMap(
                getResponse(emptyInstanceGroup(), emptyInstanceGroup())).entrySet().size());
        assertEquals(1, InstanceUtil.getInstanceStatusMap(
                getResponse(emptyInstanceGroup(), emptyInstanceGroup(), instanceGroup())).entrySet().size());
    }

    private StackV4Response getResponse(InstanceGroupV4Response... groups) {
        StackV4Response response = new StackV4Response();
        response.setInstanceGroups(Lists.newArrayList());
        response.getInstanceGroups().addAll(Arrays.asList(groups));
        return response;
    }

    private InstanceGroupV4Response emptyInstanceGroup() {
        InstanceGroupV4Response group = new InstanceGroupV4Response();
        fillWithNullInstanceIds(group);
        return group;
    }

    private void fillWithNullInstanceIds(InstanceGroupV4Response group) {
        group.setMetadata(Sets.newHashSet());
        InstanceMetaDataV4Response metaDataV4Response = new InstanceMetaDataV4Response();
        metaDataV4Response.setInstanceId(null);
        InstanceMetaDataV4Response metaDataV4Response2 = new InstanceMetaDataV4Response();
        metaDataV4Response2.setInstanceId(null);
        group.getMetadata().add(metaDataV4Response);
    }

    private InstanceGroupV4Response instanceGroup() {
        InstanceGroupV4Response group = new InstanceGroupV4Response();
        fillWithNullInstanceIds(group);
        InstanceMetaDataV4Response metaDataV4Response3 = new InstanceMetaDataV4Response();
        metaDataV4Response3.setInstanceId("instanceId");
        group.getMetadata().add(metaDataV4Response3);
        return group;
    }
}

package com.sequenceiq.it.cloudbreak.assertion.util;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.CORE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY_PRIMARY;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

class InstanceIPCollectorUtilTest {

    @Test
    void testPublicIpIsUsedWhenRequestedAndAvailable() {
        List<InstanceGroupV4Response> groups = List.of(group("master", GATEWAY, instance("10.0.0.1", "1.1.1.1", GATEWAY_PRIMARY)));

        assertThat(InstanceIPCollectorUtil.getAllInstanceIps(groups, true)).containsExactly("1.1.1.1");
    }

    @Test
    void testPrivateIpIsUsedWhenPublicIpIsNotRequested() {
        List<InstanceGroupV4Response> groups = List.of(group("master", GATEWAY, instance("10.0.0.1", "1.1.1.1", GATEWAY_PRIMARY)));

        assertThat(InstanceIPCollectorUtil.getAllInstanceIps(groups, false)).containsExactly("10.0.0.1");
    }

    @Test
    void testPrivateIpIsUsedWhenPublicIpIsMissing() {
        List<InstanceGroupV4Response> groups = List.of(group("worker", InstanceGroupType.CORE,
                instance("10.0.0.1", null, CORE), instance("10.0.0.2", "", CORE), instance("10.0.0.3", "N/A", CORE)));

        assertThat(InstanceIPCollectorUtil.getAllInstanceIps(groups, true)).containsExactly("10.0.0.1", "10.0.0.2", "10.0.0.3");
    }

    @Test
    void testMissingInstanceGroupsAndMetadataAreSkipped() {
        InstanceGroupV4Response noMetadata = group("empty", GATEWAY);
        noMetadata.setMetadata(null);
        List<InstanceGroupV4Response> groups = Arrays.asList(null, noMetadata, group("worker", InstanceGroupType.CORE, instance("10.0.0.2", null, CORE)));

        assertThat(InstanceIPCollectorUtil.getAllInstanceIps(groups, true)).containsExactly("10.0.0.2");
    }

    @Test
    void testInstanceWithoutIpFailsTheCollection() {
        InstanceMetaDataV4Response withoutIp = instance(null, "N/A", CORE);
        withoutIp.setInstanceId("i-123");
        List<InstanceGroupV4Response> groups = List.of(group("worker", InstanceGroupType.CORE, withoutIp, instance("10.0.0.2", null, CORE)));

        TestFailException exception = assertThrows(TestFailException.class, () -> InstanceIPCollectorUtil.getAllInstanceIps(groups, true));

        assertThat(exception.getMessage()).isEqualTo("The IP of instance [i-123] cannot be collected, it has no private IP and no available public IP");
    }

    @Test
    void testGetAllReachableInstanceIpsSkipsInstancesWithoutIp() {
        List<InstanceGroupV4Response> groups = List.of(group("worker", InstanceGroupType.CORE,
                instance("10.0.0.1", null, CORE), instance(null, "N/A", CORE), instance("10.0.0.3", "3.3.3.3", CORE)));

        assertThat(InstanceIPCollectorUtil.getAllReachableInstanceIps(groups, true)).containsExactly("10.0.0.1", "3.3.3.3");
    }

    @Test
    void testGetAllReachableFreeIpaInstanceIpsSkipsInstancesWithoutIp() {
        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType primary =
                com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType.GATEWAY_PRIMARY;
        List<InstanceGroupResponse> groups = List.of(freeIpaGroup(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType.MASTER,
                freeIpaInstance("10.0.0.1", null, primary), freeIpaInstance("", "", primary)));

        assertThat(InstanceIPCollectorUtil.getAllReachableFreeIpaInstanceIps(groups, true)).containsExactly("10.0.0.1");
    }

    @Test
    void testGetInstanceGroupIpsFollowsTheOrderOfHostGroupNames() {
        List<InstanceGroupV4Response> groups = List.of(
                group("master", GATEWAY, instance("10.0.0.1", null, GATEWAY_PRIMARY)),
                group("worker", InstanceGroupType.CORE, instance("10.0.0.2", null, CORE)),
                group("compute", InstanceGroupType.CORE, instance("10.0.0.3", null, CORE)));

        assertThat(InstanceIPCollectorUtil.getInstanceGroupIps(groups, List.of("compute", "master"), false)).containsExactly("10.0.0.3", "10.0.0.1");
    }

    @Test
    void testGetInstanceGroupIpsIgnoresUnknownHostGroup() {
        List<InstanceGroupV4Response> groups = List.of(group("master", GATEWAY, instance("10.0.0.1", null, GATEWAY_PRIMARY)));

        assertThat(InstanceIPCollectorUtil.getInstanceGroupIps(groups, List.of("unknown"), false)).isEmpty();
    }

    @Test
    void testGetGatewayIpsOnlyContainsGatewayGroups() {
        List<InstanceGroupV4Response> groups = List.of(
                group("master", GATEWAY, instance("10.0.0.1", "1.1.1.1", GATEWAY_PRIMARY), instance("10.0.0.2", "1.1.1.2", InstanceMetadataType.GATEWAY)),
                group("worker", InstanceGroupType.CORE, instance("10.0.0.3", "1.1.1.3", CORE)));

        assertThat(InstanceIPCollectorUtil.getGatewayIps(groups, true)).containsExactly("1.1.1.1", "1.1.1.2");
    }

    @Test
    void testGetPrimaryGatewayIpsOnlyContainsPrimaryGatewayInstances() {
        List<InstanceGroupV4Response> groups = List.of(
                group("master", GATEWAY, instance("10.0.0.1", null, GATEWAY_PRIMARY), instance("10.0.0.2", null, InstanceMetadataType.GATEWAY)),
                group("worker", InstanceGroupType.CORE, instance("10.0.0.3", null, CORE)));

        assertThat(InstanceIPCollectorUtil.getPrimaryGatewayIps(groups, false)).containsExactly("10.0.0.1");
    }

    @Test
    void testGetAllFreeIpaInstanceIpsSelectsPublicOrPrivateIp() {
        List<InstanceGroupResponse> groups = List.of(freeIpaGroup(
                com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType.MASTER,
                freeIpaInstance("10.0.0.1", "1.1.1.1", com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType.GATEWAY_PRIMARY),
                freeIpaInstance("10.0.0.2", "N/A", com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType.GATEWAY)));

        assertThat(InstanceIPCollectorUtil.getAllFreeIpaInstanceIps(groups, true)).containsExactly("1.1.1.1", "10.0.0.2");
        assertThat(InstanceIPCollectorUtil.getAllFreeIpaInstanceIps(groups, false)).containsExactly("10.0.0.1", "10.0.0.2");
    }

    @Test
    void testGetFreeIpaMasterInstanceIpsFiltersByGroupAndInstanceType() {
        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType primary =
                com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType.GATEWAY_PRIMARY;
        List<InstanceGroupResponse> groups = List.of(
                freeIpaGroup(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType.MASTER,
                        freeIpaInstance("10.0.0.1", null, primary),
                        freeIpaInstance("10.0.0.2", null, com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType.GATEWAY)),
                freeIpaGroup(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType.SLAVE,
                        freeIpaInstance("10.0.0.3", null, primary)));

        assertThat(InstanceIPCollectorUtil.getFreeIpaMasterInstanceIps(groups, primary, false)).containsExactly("10.0.0.1");
    }

    private InstanceGroupV4Response group(String name, InstanceGroupType type, InstanceMetaDataV4Response... instances) {
        InstanceGroupV4Response group = new InstanceGroupV4Response();
        group.setName(name);
        group.setType(type);
        group.setMetadata(new LinkedHashSet<>(Arrays.asList(instances)));
        return group;
    }

    private InstanceMetaDataV4Response instance(String privateIp, String publicIp, InstanceMetadataType type) {
        InstanceMetaDataV4Response instance = new InstanceMetaDataV4Response();
        instance.setPrivateIp(privateIp);
        instance.setPublicIp(publicIp);
        instance.setInstanceType(type);
        return instance;
    }

    private InstanceGroupResponse freeIpaGroup(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType type,
            InstanceMetaDataResponse... instances) {
        InstanceGroupResponse group = new InstanceGroupResponse();
        group.setType(type);
        Set<InstanceMetaDataResponse> metaData = new LinkedHashSet<>(Arrays.asList(instances));
        group.setMetaData(metaData);
        return group;
    }

    private InstanceMetaDataResponse freeIpaInstance(String privateIp, String publicIp,
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType type) {
        InstanceMetaDataResponse instance = new InstanceMetaDataResponse();
        instance.setPrivateIp(privateIp);
        instance.setPublicIp(publicIp);
        instance.setInstanceType(type);
        return instance;
    }
}

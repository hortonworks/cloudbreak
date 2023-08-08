package com.sequenceiq.cloudbreak.converter;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.InstanceResource;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.Resource;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ServiceType;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.TERMINATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.StatusChange;
import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.Sync;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;

class StackDtoToMeteringEventConverterTest {

    private StackDtoToMeteringEventConverter underTest = new StackDtoToMeteringEventConverter();

    @Test
    void testConvertToSyncEvent() {
        StackDto stack = stack();
        MeteringEvent meteringEvent = underTest.convertToSyncEvent(stack);
        assertBase(stack, meteringEvent);
        Sync sync = meteringEvent.getSync();
        assertNotNull(sync);
        assertResources(sync.getResourcesList());
    }

    @Test
    void testConvertToStatusChangeEvent() {
        StackDto stack = stack();
        MeteringEvent meteringEvent = underTest.convertToStatusChangeEvent(stack, ClusterStatus.Value.SCALE_UP);
        assertBase(stack, meteringEvent);
        StatusChange statusChange = meteringEvent.getStatusChange();
        assertNotNull(statusChange);
        assertEquals(ClusterStatus.Value.SCALE_UP, statusChange.getStatus());
        assertResources(statusChange.getResourcesList());
    }

    private void assertBase(StackDto stack, MeteringEvent meteringEvent) {
        assertNotNull(meteringEvent.getId());
        assertNotNull(meteringEvent.getTimestamp());
        assertEquals(1, meteringEvent.getVersion());
        assertEquals(ServiceType.Value.DATAHUB, meteringEvent.getServiceType());
        assertEquals(stack.getResourceCrn(), meteringEvent.getResourceCrn());
        assertEquals(stack.getEnvironmentCrn(), meteringEvent.getEnvironmentCrn());
    }

    private static void assertResources(List<Resource> resources) {
        assertThat(resources).hasSize(3);
        assertThat(resources)
                .extracting(Resource::getId)
                .contains("1", "3", "4");
        List<InstanceResource> instanceResources = resources.stream().map(Resource::getInstanceResource).toList();
        assertThat(instanceResources)
                .extracting(InstanceResource::getIpAddress, InstanceResource::getInstanceType)
                .contains(
                        tuple("1.2.3.1", "xl"),
                        tuple("1.2.3.2", "xxl"),
                        tuple("1.2.3.3", "xxl"));
    }

    private StackDto stack() {
        Stack stack = new Stack();
        stack.setResourceCrn("resourceCrn");
        stack.setEnvironmentCrn("environmentCrn");
        Map<String, InstanceGroupDto> instanceGroups = new HashMap<>();
        instanceGroups.put("master", new InstanceGroupDto(instanceGroup("xl"),
                List.of(instanceMetaData("1", "1.2.3.1", SERVICES_RUNNING), instanceMetaData("2", "1.2.3.5", TERMINATED))));
        instanceGroups.put("worker", new InstanceGroupDto(instanceGroup("xxl"),
                List.of(instanceMetaData("3", "1.2.3.2", SERVICES_RUNNING), instanceMetaData("4", "1.2.3.3", SERVICES_UNHEALTHY))));

        StackDto stackDto = new StackDto(stack, null, null, null, null, null, instanceGroups, null, null,
                null, null, null, null, null, null, null, null);
        return stackDto;
    }

    private InstanceGroup instanceGroup(String instanceType) {
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        template.setInstanceType(instanceType);
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }

    private InstanceMetaData instanceMetaData(String instanceId, String privateIp, InstanceStatus instanceStatus) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(instanceId);
        instanceMetaData.setPrivateIp(privateIp);
        instanceMetaData.setInstanceStatus(instanceStatus);
        return instanceMetaData;
    }
}
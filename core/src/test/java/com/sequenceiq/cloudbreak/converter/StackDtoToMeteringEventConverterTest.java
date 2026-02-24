package com.sequenceiq.cloudbreak.converter;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus.Value.SCALE_UP;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.InstanceResource;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.Resource;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ServiceType;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ServiceType.Value.DATAHUB;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ServiceType.Value.DATA_WAREHOUSE;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ServiceType.Value.OPERATIONAL_DB;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.TERMINATED;
import static com.sequenceiq.cloudbreak.converter.StackDtoToMeteringEventConverter.CLOUDERA_EXTERNAL_RESOURCE_NAME_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.StatusChange;
import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.Sync;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.tag.ClusterTemplateApplicationTag;
import com.sequenceiq.common.model.DefaultApplicationTag;

class StackDtoToMeteringEventConverterTest {

    private static final String OPDB_CRN = "crn:cdp:opdb:us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd:opdb:e7b1345f-4ae1-4594-9113-xc91322esi7t";

    private static final String SERVICE_FEATURE_VALUE = "Nifi";

    private static final String RESOURCE_CRN = "resourceCrn";

    private StackDtoToMeteringEventConverter underTest = new StackDtoToMeteringEventConverter();

    @Test
    void testConvertToSyncEvent() {
        StackDto stack = stack();
        MeteringEvent meteringEvent = underTest.convertToSyncEvent(stack);
        assertBase(stack, meteringEvent, DATAHUB);
        assertEquals(stack.getResourceCrn(), meteringEvent.getResourceCrn());
        Sync sync = meteringEvent.getSync();
        assertNotNull(sync);
        assertResources(sync.getResourcesList());
    }

    @Test
    void testConvertToStatusChangeEvent() {
        StackDto stack = stack();
        MeteringEvent meteringEvent = underTest.convertToStatusChangeEvent(stack, SCALE_UP);
        assertBase(stack, meteringEvent, DATAHUB);
        assertEquals(stack.getResourceCrn(), meteringEvent.getResourceCrn());
        StatusChange statusChange = meteringEvent.getStatusChange();
        assertNotNull(statusChange);
        assertEquals(SCALE_UP, statusChange.getStatus());
        assertResources(statusChange.getResourcesList());
    }

    @Test
    void testConvertWhenTagsNotEmptyAndNoServiceTypeInTags() {
        StackDto stack = stack(new Json(new StackTags(Map.of(), Map.of("APPLOCATION_TAG_KEY", "APPLICATION_TAG_VALUE"), Map.of())));
        MeteringEvent meteringEvent = underTest.convertToStatusChangeEvent(stack, SCALE_UP);
        assertBase(stack, meteringEvent, DATAHUB);
        assertEquals(stack.getResourceCrn(), meteringEvent.getResourceCrn());
        StatusChange statusChange = meteringEvent.getStatusChange();
        assertNotNull(statusChange);
        assertEquals(SCALE_UP, statusChange.getStatus());
        assertResources(statusChange.getResourcesList());
    }

    @Test
    void testConvertWhenTagsNotEmptyAndInvalidServiceTypeInTags() {
        StackDto stack = stack(new Json(new StackTags(Map.of(), Map.of(ClusterTemplateApplicationTag.SERVICE_TYPE.key(), "INVALID"), Map.of())));
        MeteringEvent meteringEvent = underTest.convertToStatusChangeEvent(stack, SCALE_UP);
        assertBase(stack, meteringEvent, DATAHUB);
        assertEquals(stack.getResourceCrn(), meteringEvent.getResourceCrn());
        StatusChange statusChange = meteringEvent.getStatusChange();
        assertNotNull(statusChange);
        assertEquals(SCALE_UP, statusChange.getStatus());
        assertResources(statusChange.getResourcesList());
    }

    @Test
    void testConvertWhenTagsNotEmptyAndValidServiceTypeInTags() {
        StackDto stack = stack(new Json(new StackTags(Map.of(),
                Map.of(ClusterTemplateApplicationTag.SERVICE_TYPE.key(), DATA_WAREHOUSE.name()), Map.of())));
        MeteringEvent meteringEvent = underTest.convertToStatusChangeEvent(stack, SCALE_UP);
        assertBase(stack, meteringEvent, DATA_WAREHOUSE);
        assertEquals(stack.getResourceCrn(), meteringEvent.getResourceCrn());
        StatusChange statusChange = meteringEvent.getStatusChange();
        assertNotNull(statusChange);
        assertEquals(SCALE_UP, statusChange.getStatus());
        assertResources(statusChange.getResourcesList());
    }

    @Test
    void testConvertServiceFeatureWhenServiceFeatureTagExists() {
        StackDto stack = stack(new Json(new StackTags(Map.of(), Map.of(ClusterTemplateApplicationTag.SERVICE_FEATURE.key(), SERVICE_FEATURE_VALUE), Map.of())));
        MeteringEvent meteringEvent = underTest.convertToStatusChangeEvent(stack, SCALE_UP);
        assertEquals(SERVICE_FEATURE_VALUE, meteringEvent.getServiceFeature());
    }

    @Test
    void testConvertServiceFeatureWhenServiceFeatureTagDoesNotExist() {
        StackDto stack = stack(new Json(new StackTags(Map.of(), Map.of(), Map.of())));
        MeteringEvent meteringEvent = underTest.convertToStatusChangeEvent(stack, SCALE_UP);
        assertEquals(StringUtils.EMPTY, meteringEvent.getServiceFeature());
    }

    @Test
    void testConvertResourceCrnWhenApplicationTagsContainsClouderaExternalResourceName() {
        StackDto stack = stack(new Json(new StackTags(Map.of(),
                Map.of(ClusterTemplateApplicationTag.SERVICE_TYPE.key(), OPERATIONAL_DB.name(), CLOUDERA_EXTERNAL_RESOURCE_NAME_TAG, OPDB_CRN), Map.of())));
        MeteringEvent meteringEvent = underTest.convertToStatusChangeEvent(stack, SCALE_UP);
        assertEquals(OPDB_CRN, meteringEvent.getResourceCrn());
    }

    @Test
    void testConvertResourceCrnWhenApplicationTagsContainsLowerCaseClouderaExternalResourceNameOnly() {
        StackDto stack = stack(new Json(new StackTags(Map.of(), Map.of(CLOUDERA_EXTERNAL_RESOURCE_NAME_TAG.toLowerCase(Locale.ROOT), OPDB_CRN), Map.of())));
        MeteringEvent meteringEvent = underTest.convertToStatusChangeEvent(stack, SCALE_UP);
        assertEquals(OPDB_CRN, meteringEvent.getResourceCrn());
    }

    @Test
    void testConvertResourceCrnWhenDefaultTagsContainsClouderaResourceNameOnly() {
        StackDto stack = stack(new Json(new StackTags(Map.of(), Map.of(), Map.of(DefaultApplicationTag.RESOURCE_CRN.key(), RESOURCE_CRN))));
        MeteringEvent meteringEvent = underTest.convertToStatusChangeEvent(stack, SCALE_UP);
        assertEquals(RESOURCE_CRN, meteringEvent.getResourceCrn());
    }

    @Test
    void testConvertResourceCrnWhenNoTags() {
        StackDto stack = stack(new Json(new StackTags(Map.of(), Map.of(), Map.of())));
        MeteringEvent meteringEvent = underTest.convertToStatusChangeEvent(stack, SCALE_UP);
        assertEquals(RESOURCE_CRN, meteringEvent.getResourceCrn());
    }

    private void assertBase(StackDto stack, MeteringEvent meteringEvent, ServiceType.Value serviceType) {
        assertNotNull(meteringEvent.getId());
        assertNotNull(meteringEvent.getTimestamp());
        assertEquals(1, meteringEvent.getVersion());
        assertEquals(serviceType, meteringEvent.getServiceType());
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
        return stack(null);
    }

    private StackDto stack(Json tags) {
        Stack stack = new Stack();
        stack.setResourceCrn(RESOURCE_CRN);
        stack.setEnvironmentCrn("environmentCrn");
        Map<String, InstanceGroupDto> instanceGroups = new HashMap<>();
        instanceGroups.put("master", new InstanceGroupDto(instanceGroup("xl"),
                List.of(instanceMetaData("1", "1.2.3.1", SERVICES_RUNNING), instanceMetaData("2", "1.2.3.5", TERMINATED))));
        instanceGroups.put("worker", new InstanceGroupDto(instanceGroup("xxl"),
                List.of(instanceMetaData("3", "1.2.3.2", SERVICES_RUNNING), instanceMetaData("4", "1.2.3.3", SERVICES_UNHEALTHY))));
        stack.setTags(tags);

        StackDto stackDto = new StackDto(stack, null, null, null, null, null, instanceGroups, null, null,
                null, null, null, null, null, null, null, null, null, NotificationState.DISABLED);
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
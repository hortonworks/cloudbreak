package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.CustomConfigurationsDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

class StructuredEventToCDPClusterShapeConverterTest {

    private StructuredEventToCDPClusterShapeConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredEventToCDPClusterShapeConverter();
    }

    @Test
    public void testConvertWithNull() {
        UsageProto.CDPClusterShape flowClusterShape = underTest.convert((StructuredFlowEvent) null);

        assertEquals("", flowClusterShape.getClusterTemplateName());
        assertEquals(-1, flowClusterShape.getNodes());
        assertEquals("", flowClusterShape.getDefinitionDetails());
        assertFalse(flowClusterShape.getTemporaryStorageUsed());

        UsageProto.CDPClusterShape syncClusterShape = underTest.convert((StructuredSyncEvent) null);

        assertEquals("", syncClusterShape.getClusterTemplateName());
        assertEquals(-1, syncClusterShape.getNodes());
        assertEquals("", syncClusterShape.getDefinitionDetails());
        assertFalse(syncClusterShape.getTemporaryStorageUsed());
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPClusterShape flowClusterShape = underTest.convert(structuredFlowEvent);

        assertEquals("", flowClusterShape.getClusterTemplateName());
        assertEquals(-1, flowClusterShape.getNodes());
        assertEquals("", flowClusterShape.getDefinitionDetails());
        assertFalse(flowClusterShape.getTemporaryStorageUsed());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPClusterShape syncClusterShape = underTest.convert(structuredSyncEvent);

        assertEquals("", syncClusterShape.getClusterTemplateName());
        assertEquals(-1, syncClusterShape.getNodes());
        assertEquals("", syncClusterShape.getDefinitionDetails());
        assertFalse(syncClusterShape.getTemporaryStorageUsed());
    }

    @Test
    public void testConversionWithValues() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(createStackDetails());
        BlueprintDetails flowBlueprintDetails = new BlueprintDetails();
        flowBlueprintDetails.setName("My Blueprint");
        structuredFlowEvent.setBlueprintDetails(flowBlueprintDetails);

        UsageProto.CDPClusterShape flowClusterShape = underTest.convert(structuredFlowEvent);

        assertEquals("My Blueprint", flowClusterShape.getClusterTemplateName());
        assertEquals(10, flowClusterShape.getNodes());
        assertEquals("compute=3, gw=4, master=1, worker=2", flowClusterShape.getHostGroupNodeCount());
        assertTrue(flowClusterShape.getTemporaryStorageUsed());
        assertEquals("{\"services\":[\"service1\",\"service2\",\"service3\"]," +
                        "\"roles\":[\"role1\",\"role2\"],\"runtimeVersion\":\"7.2.15\"}", flowClusterShape.getClusterTemplateOverridesDetails());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(createStackDetails());
        BlueprintDetails syncBlueprintDetails = new BlueprintDetails();
        syncBlueprintDetails.setName("My Blueprint");
        structuredSyncEvent.setBlueprintDetails(syncBlueprintDetails);

        UsageProto.CDPClusterShape syncClusterShape = underTest.convert(structuredSyncEvent);

        assertEquals("My Blueprint", syncClusterShape.getClusterTemplateName());
        assertEquals(10, syncClusterShape.getNodes());
        assertEquals("compute=3, gw=4, master=1, worker=2", syncClusterShape.getHostGroupNodeCount());
        assertTrue(syncClusterShape.getTemporaryStorageUsed());
        assertEquals("{\"services\":[\"service1\",\"service2\",\"service3\"]," +
                "\"roles\":[\"role1\",\"role2\"],\"runtimeVersion\":\"7.2.15\"}", syncClusterShape.getClusterTemplateOverridesDetails());
    }

    @Test
    public void testConversionWithBlueprintDetails() {
        String blueprintJson = "{ \"cdhVersion\": \"7.2.14\", \"displayName\": \"dataengineering ha\", \"services\": [{ \"refName\": \"zookeeper\", " +
                "\"serviceType\": \"ZOOKEEPER\", \"roleConfigGroups\": [{ \"refName\": \"zookeeper-SERVER-BASE\", \"roleType\": \"SERVER\", \"base\": true " +
                "}]},{ \"refName\": \"hdfs\", \"serviceType\": \"HDFS\", \"serviceConfigs\": [{ \"name\": \"zookeeper_service\", \"ref\": \"zookeeper\" }]}," +
                "{ \"refName\": \"hms\", \"serviceType\": \"HIVE\", \"displayName\": \"Hive Metastore\", \"roleConfigGroups\": [{ \"refName\": " +
                "\"hms-GATEWAY-BASE\", \"roleType\": \"GATEWAY\", \"base\": true }] }, { \"refName\": \"hive\", \"serviceType\": \"HIVE_ON_TEZ\", " +
                "\"displayName\": \"Hive\", \"serviceConfigs\": [{ \"name\": \"tez_auto_reducer_parallelism\", \"value\": \"false\" }] }, " +
                "{ \"refName\": \"hue\", \"serviceType\": \"HUE\", \"serviceConfigs\": [{ \"name\": \"hue_service_safety_valve\", \"value\": \"[desktop]" +
                "\\napp_blacklist=spark,zookeeper,hbase,impala,search,sqoop,security,pig\" }] }, { \"refName\": \"livy\", \"serviceType\": \"LIVY\", " +
                "\"roleConfigGroups\": [{ \"refName\": \"livy-GATEWAY-BASE\", \"roleType\": \"GATEWAY\", \"base\": true }] }, { \"refName\": \"oozie\", " +
                "\"serviceType\": \"OOZIE\", \"roleConfigGroups\": [{ \"refName\": \"oozie-OOZIE_SERVER-BASE\", \"roleType\": \"OOZIE_SERVER\", " +
                "\"base\": true }] }, { \"refName\": \"sqoop\", \"serviceType\": \"SQOOP_CLIENT\", \"roleConfigGroups\": [{ \"refName\": " +
                "\"sqoop-SQOOP_CLIENT-GATEWAY-BASE\", \"roleType\": \"GATEWAY\", \"configs\": [], \"base\": true }] }, { \"refName\": \"yarn\", " +
                "\"serviceType\": \"YARN\", \"serviceConfigs\": [{ \"name\": \"yarn_admin_acl\", \"value\": \"yarn,hive,hdfs,mapred\" }] }, { \"refName\": " +
                "\"spark_on_yarn\", \"serviceType\": \"SPARK_ON_YARN\", \"roleConfigGroups\": [{ \"refName\": " +
                "\"spark_on_yarn-SPARK_YARN_HISTORY_SERVER-BASE\", \"roleType\": \"SPARK_YARN_HISTORY_SERVER\", \"base\": true }] }, { \"refName\": \"tez\"," +
                "\"serviceType\": \"TEZ\", \"roleConfigGroups\": [{ \"refName\": \"tez-GATEWAY-BASE\", \"roleType\": \"GATEWAY\", \"base\": true }] }, " +
                "{ \"refName\": \"das\", \"serviceType\": \"DAS\" }] }";
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(createStackDetails());
        BlueprintDetails flowBlueprintDetails = new BlueprintDetails();
        flowBlueprintDetails.setName("My Blueprint");
        flowBlueprintDetails.setBlueprintJson(blueprintJson);
        structuredFlowEvent.setBlueprintDetails(flowBlueprintDetails);

        UsageProto.CDPClusterShape flowClusterShape = underTest.convert(structuredFlowEvent);

        assertEquals("My Blueprint", flowClusterShape.getClusterTemplateName());
        assertEquals(10, flowClusterShape.getNodes());
        assertEquals("compute=3, gw=4, master=1, worker=2", flowClusterShape.getHostGroupNodeCount());
        assertTrue(flowClusterShape.getTemporaryStorageUsed());
        assertEquals("{\"services\":{\"HIVE_ON_TEZ\":1,\"HIVE\":1,\"LIVY\":1,\"DAS\":1,\"HDFS\":1," +
                "\"OOZIE\":1,\"TEZ\":1,\"HUE\":1,\"SQOOP_CLIENT\":1,\"ZOOKEEPER\":1,\"YARN\":1,\"SPARK_ON_YARN\":1}}",
                flowClusterShape.getClusterTemplateDetails());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(createStackDetails());
        BlueprintDetails syncBlueprintDetails = new BlueprintDetails();
        syncBlueprintDetails.setName("My Blueprint");
        syncBlueprintDetails.setBlueprintJson(blueprintJson);
        structuredSyncEvent.setBlueprintDetails(syncBlueprintDetails);

        UsageProto.CDPClusterShape syncClusterShape = underTest.convert(structuredSyncEvent);

        assertEquals("My Blueprint", syncClusterShape.getClusterTemplateName());
        assertEquals(10, syncClusterShape.getNodes());
        assertEquals("compute=3, gw=4, master=1, worker=2", syncClusterShape.getHostGroupNodeCount());
        assertTrue(syncClusterShape.getTemporaryStorageUsed());
        assertEquals("{\"services\":{\"HIVE_ON_TEZ\":1,\"HIVE\":1,\"LIVY\":1,\"DAS\":1,\"HDFS\":1," +
                        "\"OOZIE\":1,\"TEZ\":1,\"HUE\":1,\"SQOOP_CLIENT\":1,\"ZOOKEEPER\":1,\"YARN\":1,\"SPARK_ON_YARN\":1}}",
                syncClusterShape.getClusterTemplateDetails());
    }

    @Test
    public void testConversionWithIncorrectBlueprintDetails() {
        String blueprintJson = "{ \"cdhVersion\": \"7.2.14\", \"displayName\": \"dataengineering ha\", \"wrongServices\": " +
                "[{ \"refName\": \"das\", \"serviceType\": \"DAS\" }] }";
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(createStackDetails());
        BlueprintDetails flowBlueprintDetails = new BlueprintDetails();
        flowBlueprintDetails.setName("My Blueprint");
        flowBlueprintDetails.setBlueprintJson(blueprintJson);
        structuredFlowEvent.setBlueprintDetails(flowBlueprintDetails);

        UsageProto.CDPClusterShape flowClusterShape = underTest.convert(structuredFlowEvent);

        assertEquals("My Blueprint", flowClusterShape.getClusterTemplateName());
        assertEquals(10, flowClusterShape.getNodes());
        assertEquals("compute=3, gw=4, master=1, worker=2", flowClusterShape.getHostGroupNodeCount());
        assertTrue(flowClusterShape.getTemporaryStorageUsed());
        assertEquals("{\"services\":{}}", flowClusterShape.getClusterTemplateDetails());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(createStackDetails());
        BlueprintDetails syncBlueprintDetails = new BlueprintDetails();
        syncBlueprintDetails.setName("My Blueprint");
        syncBlueprintDetails.setBlueprintJson(blueprintJson);
        structuredSyncEvent.setBlueprintDetails(syncBlueprintDetails);

        UsageProto.CDPClusterShape syncClusterShape = underTest.convert(structuredSyncEvent);

        assertEquals("My Blueprint", syncClusterShape.getClusterTemplateName());
        assertEquals(10, syncClusterShape.getNodes());
        assertEquals("compute=3, gw=4, master=1, worker=2", syncClusterShape.getHostGroupNodeCount());
        assertTrue(syncClusterShape.getTemporaryStorageUsed());
        assertEquals("{\"services\":{}}", syncClusterShape.getClusterTemplateDetails());
    }

    @Test
    public void testConversionWithoutCustomConfigurations() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        StackDetails stackDetails = new StackDetails();
        InstanceGroupDetails master = createInstanceGroupDetails("master", 2, null);
        stackDetails.setInstanceGroups(List.of(master));
        structuredFlowEvent.setStack(stackDetails);

        UsageProto.CDPClusterShape flowClusterShape = underTest.convert(structuredFlowEvent);

        assertEquals(2, flowClusterShape.getNodes());
        assertEquals("master=2", flowClusterShape.getHostGroupNodeCount());
        assertEquals("null", flowClusterShape.getClusterTemplateOverridesDetails());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);

        UsageProto.CDPClusterShape syncClusterShape = underTest.convert(structuredSyncEvent);

        assertEquals(2, syncClusterShape.getNodes());
        assertEquals("master=2", syncClusterShape.getHostGroupNodeCount());
        assertEquals("null", syncClusterShape.getClusterTemplateOverridesDetails());
    }

    @Test
    public void testConversionWithoutTemporaryStorage() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        StackDetails stackDetails = new StackDetails();
        InstanceGroupDetails master = createInstanceGroupDetails("master", 2, null);
        stackDetails.setInstanceGroups(List.of(master));
        structuredFlowEvent.setStack(stackDetails);

        UsageProto.CDPClusterShape flowClusterShape = underTest.convert(structuredFlowEvent);

        assertEquals(2, flowClusterShape.getNodes());
        assertEquals("master=2", flowClusterShape.getHostGroupNodeCount());
        assertFalse(flowClusterShape.getTemporaryStorageUsed());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);

        UsageProto.CDPClusterShape syncClusterShape = underTest.convert(structuredSyncEvent);

        assertEquals(2, syncClusterShape.getNodes());
        assertEquals("master=2", syncClusterShape.getHostGroupNodeCount());
        assertFalse(syncClusterShape.getTemporaryStorageUsed());
    }

    @Test
    public void testLengthLimitedDefinitionDetails() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPClusterShape flowClusterShape = underTest.convert(structuredFlowEvent);

        assertEquals("", flowClusterShape.getDefinitionDetails());

        StackDetails stackDetails = new StackDetails();
        InstanceGroupDetails master = createInstanceGroupDetails("master", 2, null);
        stackDetails.setInstanceGroups(List.of(master));
        structuredFlowEvent.setStack(stackDetails);

        flowClusterShape = underTest.convert(structuredFlowEvent);

        assertEquals("[{\"groupName\":\"master\",\"nodeCount\":2,\"volumes\":[],\"attributes\":{},\"runningInstances\":{}}]",
                flowClusterShape.getDefinitionDetails());

        master = createInstanceGroupDetails("master", 2, null);
        stackDetails.setInstanceGroups(Collections.nCopies(10, master));
        structuredFlowEvent.setStack(stackDetails);

        flowClusterShape = underTest.convert(structuredFlowEvent);

        int definitionLength = StringUtils.length(flowClusterShape.getDefinitionDetails());
        assertTrue(definitionLength >= 0 && definitionLength <= 3000);

        master = createInstanceGroupDetails("master", 2, null);
        stackDetails.setInstanceGroups(Collections.nCopies(110, master));
        structuredFlowEvent.setStack(stackDetails);

        flowClusterShape = underTest.convert(structuredFlowEvent);

        assertEquals("", flowClusterShape.getDefinitionDetails());
    }

    private StackDetails createStackDetails() {
        StackDetails stackDetails = new StackDetails();
        InstanceGroupDetails master = createInstanceGroupDetails("master", 1, TemporaryStorage.ATTACHED_VOLUMES.name());
        InstanceGroupDetails worker = createInstanceGroupDetails("worker", 2, TemporaryStorage.ATTACHED_VOLUMES.name());
        InstanceGroupDetails compute = createInstanceGroupDetails("compute", 3, TemporaryStorage.EPHEMERAL_VOLUMES.name());
        InstanceGroupDetails gw = createInstanceGroupDetails("gw", 4, TemporaryStorage.ATTACHED_VOLUMES.name());

        stackDetails.setInstanceGroups(List.of(master, worker, compute, gw));
        stackDetails.setCustomConfigurations(createCustomConfigurationsDetails());
        return stackDetails;
    }

    private InstanceGroupDetails createInstanceGroupDetails(String groupName, int nodeCount, String storage) {
        InstanceGroupDetails ig = new InstanceGroupDetails();
        ig.setGroupName(groupName);
        ig.setNodeCount(nodeCount);
        ig.setTemporaryStorage(storage);
        return ig;
    }

    private CustomConfigurationsDetails createCustomConfigurationsDetails() {
        CustomConfigurationsDetails customConfigurationsDetails = new CustomConfigurationsDetails();
        customConfigurationsDetails.setServices(List.of("service1", "service2", "service3"));
        customConfigurationsDetails.setRoles(List.of("role1", "role2"));
        customConfigurationsDetails.setRuntimeVersion("7.2.15");
        return customConfigurationsDetails;
    }
}
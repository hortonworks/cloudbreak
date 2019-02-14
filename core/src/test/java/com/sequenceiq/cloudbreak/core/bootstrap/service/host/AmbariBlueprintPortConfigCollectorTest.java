package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.FileReaderUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.AmbariBlueprintPortConfigCollector.PortConfig;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;

public class AmbariBlueprintPortConfigCollectorTest {

    private static final int HIVE_PORT = 123;

    private static final int ZEPPELIN_PORT = 213;

    private static final int YARN_PORT = 321;

    private final AmbariBlueprintPortConfigCollector underTest = new AmbariBlueprintPortConfigCollector();

    @Before
    public void setup() {
        List<PortConfig> blueprintServicePorts = new ArrayList<>();
        blueprintServicePorts.add(new PortConfig("HIVE_SERVER", "hive-site", "hive.server2.thrift.http.port", null));
        blueprintServicePorts.add(new PortConfig("ZEPPELIN", "zeppelin-config", "zeppelin.server.port", null));
        blueprintServicePorts.add(new PortConfig("RESOURCEMANAGER_WEB", "yarn-site", null, "yarn.resourcemanager.webapp.address"));
        underTest.setBlueprintServicePorts(blueprintServicePorts);
    }

    @Test
    public void testGetServicePorts() {
        String blueprintText = FileReaderUtil.readResourceFile(this, "test-blueprint.json");
        ClusterDefinition clusterDefinition = new ClusterDefinition();
        clusterDefinition.setClusterDefinitionText(blueprintText);
        Map<String, Integer> result = underTest.getServicePorts(clusterDefinition);
        assertEquals(HIVE_PORT, result.get(ExposedService.HIVE_SERVER.getKnoxService()).intValue());
        assertEquals(ZEPPELIN_PORT, result.get(ExposedService.ZEPPELIN.getKnoxService()).intValue());
        assertEquals(YARN_PORT, result.get(ExposedService.RESOURCEMANAGER_WEB.getKnoxService()).intValue());
        assertEquals(ExposedService.BEACON_SERVER.getDefaultPort(), result.get(ExposedService.BEACON_SERVER.getKnoxService()));
        assertEquals(ExposedService.AMBARI.getDefaultPort(), result.get(ExposedService.AMBARI.getKnoxService()));
        assertEquals(ExposedService.RANGER.getDefaultPort(), result.get(ExposedService.RANGER.getKnoxService()));
        assertEquals(ExposedService.ATLAS.getDefaultPort(), result.get(ExposedService.ATLAS.getKnoxService()));
        assertEquals(ExposedService.SPARK_HISTORY_SERVER.getDefaultPort(), result.get(ExposedService.SPARK_HISTORY_SERVER.getKnoxService()));
    }

    @Test
    public void testWithissingConfigArray() {
        String blueprintText = FileReaderUtil.readResourceFile(this, "test-blueprint-no-config.json");
        ClusterDefinition clusterDefinition = new ClusterDefinition();
        clusterDefinition.setClusterDefinitionText(blueprintText);
        Map<String, Integer> result = underTest.getServicePorts(clusterDefinition);
        assertEquals(ExposedService.HIVE_SERVER.getDefaultPort(), result.get(ExposedService.HIVE_SERVER.getKnoxService()));
        assertEquals(ExposedService.ZEPPELIN.getDefaultPort(), result.get(ExposedService.ZEPPELIN.getKnoxService()));
        assertEquals(ExposedService.RESOURCEMANAGER_WEB.getDefaultPort(), result.get(ExposedService.RESOURCEMANAGER_WEB.getKnoxService()));
        assertEquals(ExposedService.BEACON_SERVER.getDefaultPort(), result.get(ExposedService.BEACON_SERVER.getKnoxService()));
        assertEquals(ExposedService.AMBARI.getDefaultPort(), result.get(ExposedService.AMBARI.getKnoxService()));
        assertEquals(ExposedService.RANGER.getDefaultPort(), result.get(ExposedService.RANGER.getKnoxService()));
        assertEquals(ExposedService.ATLAS.getDefaultPort(), result.get(ExposedService.ATLAS.getKnoxService()));
        assertEquals(ExposedService.SPARK_HISTORY_SERVER.getDefaultPort(), result.get(ExposedService.SPARK_HISTORY_SERVER.getKnoxService()));
    }
}
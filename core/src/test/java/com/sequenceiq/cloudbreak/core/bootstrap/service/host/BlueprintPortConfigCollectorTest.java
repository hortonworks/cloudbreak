package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.FileReaderUtil;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.BlueprintPortConfigCollector.PortConfig;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintPortConfigCollectorTest {

    private static final int HIVE_PORT = 123;

    private static final int ZEPPELIN_PORT = 213;

    private static final int YARN_PORT = 321;

    @InjectMocks
    private BlueprintPortConfigCollector underTest;

    @Mock
    private SecretService secretService;

    @Before
    public void setup() {
        when(secretService.get(anyString())).thenAnswer(it -> it.getArgument(0));
        List<PortConfig> blueprintServicePorts = new ArrayList<>();
        blueprintServicePorts.add(new PortConfig("HIVE_SERVER", "hive-site", "hive.server2.thrift.http.port", null));
        blueprintServicePorts.add(new PortConfig("ZEPPELIN", "zeppelin-config", "zeppelin.server.port", null));
        blueprintServicePorts.add(new PortConfig("RESOURCEMANAGER_WEB", "yarn-site", null, "yarn.resourcemanager.webapp.address"));
        underTest.setBlueprintServicePorts(blueprintServicePorts);
    }

    @Test
    public void testGetServicePorts() {
        String blueprintText = FileReaderUtil.readResourceFile(this, "test-blueprint.json");
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(blueprintText);
        Map<String, Integer> result = underTest.getServicePorts(blueprint);
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
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(blueprintText);
        Map<String, Integer> result = underTest.getServicePorts(blueprint);
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
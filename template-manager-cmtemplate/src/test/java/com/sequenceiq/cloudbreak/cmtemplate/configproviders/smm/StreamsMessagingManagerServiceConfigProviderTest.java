package com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class StreamsMessagingManagerServiceConfigProviderTest {

    private final StreamsMessagingManagerServiceConfigProvider underTest = new StreamsMessagingManagerServiceConfigProvider();

    @Test
    public void testGetStreamsMessagingManagerServiceConfigsWhenInternalFqdn() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject("cm.fqdn.host");
        String inputJson = getBlueprintText("input/cdp-streaming.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> streamsMessagingManagerServiceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(3, streamsMessagingManagerServiceConfigs.size());
        assertEquals("cm.metrics.host", streamsMessagingManagerServiceConfigs.get(0).getName());
        assertEquals("cm.fqdn.host", streamsMessagingManagerServiceConfigs.get(0).getValue());

        assertEquals("cm.metrics.username", streamsMessagingManagerServiceConfigs.get(1).getName());
        assertEquals("cbambariuser", streamsMessagingManagerServiceConfigs.get(1).getValue());

        assertEquals("cm.metrics.password", streamsMessagingManagerServiceConfigs.get(2).getName());
        assertEquals("cbambaripassword", streamsMessagingManagerServiceConfigs.get(2).getValue());
    }

    @Test
    public void testGetStreamsMessagingManagerServiceConfigsWhenNoInternalFqdn() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(null);
        String inputJson = getBlueprintText("input/cdp-streaming.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> streamsMessagingManagerServiceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(3, streamsMessagingManagerServiceConfigs.size());
        assertEquals("cm.metrics.host", streamsMessagingManagerServiceConfigs.get(0).getName());
        assertEquals("122.0.0.1", streamsMessagingManagerServiceConfigs.get(0).getValue());
    }

    @Test
    public void testGetStreamsMessagingManagerServerRoleConfigs() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(null);
        String inputJson = getBlueprintText("input/cdp-streaming.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> streamsMessagingManager = roleConfigs.get("streams_messaging_manager-STREAMS_MESSAGING_MANAGER_SERVER-BASE");

        assertEquals(3, streamsMessagingManager.size());
        assertEquals("streams.messaging.manager.storage.connector.connectURI", streamsMessagingManager.get(0).getName());
        assertEquals("jdbc:postgresql://testhost:5432/smm", streamsMessagingManager.get(0).getValue());

        assertEquals("streams.messaging.manager.storage.connector.user", streamsMessagingManager.get(1).getName());
        assertEquals("smm_server", streamsMessagingManager.get(1).getValue());

        assertEquals("streams.messaging.manager.storage.connector.password", streamsMessagingManager.get(2).getName());
        assertEquals("smm_server_db_password", streamsMessagingManager.get(2).getValue());
    }

    private TemplatePreparationObject getTemplatePreparationObject(String internalFqdn) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 3);

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(DatabaseType.STREAMS_MESSAGING_MANAGER.toString());
        rdsConfig.setConnectionUserName("smm_server");
        rdsConfig.setConnectionPassword("smm_server_db_password");
        rdsConfig.setConnectionURL("jdbc:postgresql://testhost:5432/smm");

        GeneralClusterConfigs gcc = new GeneralClusterConfigs();
        gcc.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.ofNullable(internalFqdn));
        gcc.setClusterManagerIp("122.0.0.1");
        gcc.setCloudbreakAmbariUser("cbambariuser");
        gcc.setCloudbreakAmbariPassword("cbambaripassword");

        return TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(master, worker))
                .withRdsConfigs(Set.of(rdsConfig))
                .withGeneralClusterConfigs(gcc)
                .build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}

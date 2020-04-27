package com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm.StreamsMessagingManagerServiceConfigProvider.DATABASE_HOST;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm.StreamsMessagingManagerServiceConfigProvider.DATABASE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm.StreamsMessagingManagerServiceConfigProvider.DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm.StreamsMessagingManagerServiceConfigProvider.DATABASE_PORT;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm.StreamsMessagingManagerServiceConfigProvider.DATABASE_TYPE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm.StreamsMessagingManagerServiceConfigProvider.DATABASE_USER;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class StreamsMessagingManagerServiceConfigProviderTest {

    private final StreamsMessagingManagerServiceConfigProvider underTest = new StreamsMessagingManagerServiceConfigProvider();

    @Test
    public void testGetStreamsMessagingManagerServerConfigs() {
        String inputJson = getBlueprintText("input/cdp-streaming.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(null, false, cmTemplateProcessor);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertThat(serviceConfigs).hasSameElementsAs(List.of(
                config(DATABASE_TYPE, "postgresql"),
                config(DATABASE_NAME, "smm"),
                config(DATABASE_HOST, "testhost"),
                config(DATABASE_PORT, "5432"),
                config(DATABASE_USER, "smm_server"),
                config(DATABASE_PASSWORD, "smm_server_db_password")
        ));
    }

    private TemplatePreparationObject getTemplatePreparationObject(String internalFqdn, boolean ssl,
            CmTemplateProcessor cmTemplateProcessor) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(null, null, null, cmTemplateProcessor);

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(DatabaseType.STREAMS_MESSAGING_MANAGER.toString());
        rdsConfig.setDatabaseEngine(DatabaseVendor.POSTGRES);
        rdsConfig.setConnectionURL("jdbc:postgresql://testhost:5432/smm");
        rdsConfig.setConnectionUserName("smm_server");
        rdsConfig.setConnectionPassword("smm_server_db_password");

        return TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withHostgroupViews(Set.of(master, worker))
                .withRdsConfigs(Set.of(rdsConfig))
                .build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}

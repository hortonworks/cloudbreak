package com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm.StreamsMessagingManagerServiceConfigProvider.DATABASE_HOST;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm.StreamsMessagingManagerServiceConfigProvider.DATABASE_JDBC_URL_OVERRIDE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm.StreamsMessagingManagerServiceConfigProvider.DATABASE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm.StreamsMessagingManagerServiceConfigProvider.DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm.StreamsMessagingManagerServiceConfigProvider.DATABASE_PORT;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm.StreamsMessagingManagerServiceConfigProvider.DATABASE_TYPE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm.StreamsMessagingManagerServiceConfigProvider.DATABASE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class StreamsMessagingManagerServiceConfigProviderTest {

    private final StreamsMessagingManagerServiceConfigProvider underTest = new StreamsMessagingManagerServiceConfigProvider();

    @Test
    public void testGetStreamsMessagingManagerServerConfigs() {
        String inputJson = getBlueprintText("input/cdp-streaming.bp").replace("__CDH_VERSION__", "7.2.0");
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

    @Test
    public void testGetStreamsMessagingManagerServerConfigsWithSsl() {
        String inputJson = getBlueprintText("input/cdp-streaming.bp").replace("__CDH_VERSION__", "7.2.0");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(null, true, cmTemplateProcessor);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertThat(serviceConfigs).hasSameElementsAs(List.of(
                config(DATABASE_TYPE, "postgresql"),
                config(DATABASE_PASSWORD, "smm_server_db_password"),
                config(DATABASE_USER, "smm_server"),
                config(DATABASE_JDBC_URL_OVERRIDE, "jdbc:postgresql://testhost:5432/smm?ssl=true&sslmode=verify-full&sslrootcert=")
        ));
    }

    private TemplatePreparationObject getTemplatePreparationObject(String internalFqdn, boolean ssl,
            CmTemplateProcessor cmTemplateProcessor) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(null, null, null, null, cmTemplateProcessor);

        RdsConfigWithoutCluster rdsConfig = mock(RdsConfigWithoutCluster.class);
        when(rdsConfig.getType()).thenReturn(DatabaseType.STREAMS_MESSAGING_MANAGER.toString());
        when(rdsConfig.getDatabaseEngine()).thenReturn(DatabaseVendor.POSTGRES);
        when(rdsConfig.getConnectionURL()).thenReturn("jdbc:postgresql://testhost:5432/smm");
        when(rdsConfig.getConnectionUserName()).thenReturn("smm_server");
        when(rdsConfig.getConnectionPassword()).thenReturn("smm_server_db_password");
        if (ssl) {
            when(rdsConfig.getSslMode()).thenReturn(RdsSslMode.ENABLED);
            when(rdsConfig.getConnectionURL()).thenReturn("jdbc:postgresql://testhost:5432/smm?ssl=true");
        }

        return TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withProductDetails(new ClouderaManagerRepo()
                        .withVersion("7.2.2"), new ArrayList<>())
                .withHostgroupViews(Set.of(master, worker))
                .withRdsViews(Set.of(rdsConfig)
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}

package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ssb;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class SqlStreamBuilderMveConfigProviderTest {

    private final SqlStreamBuilderMveConfigProvider underTest = new SqlStreamBuilderMveConfigProvider();

    @Test
    void testNoConfigNeeded() {
        CmTemplateProcessor cmTemplateProcessor = initTemplateProcessor("7.2.10");
        TemplatePreparationObject preparationObject = initTemplatePreparationObject(cmTemplateProcessor);

        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject));
    }

    @Test
    void testConfigNeeded() {
        CmTemplateProcessor cmTemplateProcessor = initTemplateProcessor("7.2.11");
        TemplatePreparationObject preparationObject = initTemplatePreparationObject(cmTemplateProcessor);

        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject));
    }

    @Test
    void testProperSnapperDbConfig() {
        CmTemplateProcessor cmTemplateProcessor = initTemplateProcessor("7.2.11");
        TemplatePreparationObject preparationObject = initTemplatePreparationObject(cmTemplateProcessor);

        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(SqlStreamBuilderRoles.MATERIALIZED_VIEW_ENGINE,
                cmTemplateProcessor, preparationObject);

        assertThat(roleConfigs).hasSameElementsAs(
                List.of(
                        config(SqlStreamBuilderMveConfigProvider.DATABASE_URL, "jdbc:postgresql://testhost:5432/ssb_mve"),
                        config(SqlStreamBuilderMveConfigProvider.DATABASE_USER, "ssb_test_user"),
                        config(SqlStreamBuilderMveConfigProvider.DATABASE_PASSWORD, "ssb_test_pw")
                ));
    }

    private CmTemplateProcessor initTemplateProcessor(String cdhVersion) {
        String json = FileReaderUtils.readFileFromClasspathQuietly("input/ssb.bp");
        json = json.replace("__CDH_VERSION__", cdhVersion);

        return new CmTemplateProcessor(json);
    }

    private TemplatePreparationObject initTemplatePreparationObject(CmTemplateProcessor cmTemplateProcessor) {
        HostgroupView manager = new HostgroupView("manager", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.CORE, 2);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(null, null, null, null, cmTemplateProcessor);

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(DatabaseType.SQL_STREAM_BUILDER_SNAPPER.toString());
        lenient().when(rdsConfig.getConnectionURL()).thenReturn("jdbc:postgresql://testhost:5432/ssb_mve");
        lenient().when(rdsConfig.getConnectionUserName()).thenReturn("ssb_test_user");
        lenient().when(rdsConfig.getConnectionPassword()).thenReturn("ssb_test_pw");

        return TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withHostgroupViews(Set.of(manager, master, worker))
                .withRdsViews(Set.of(rdsConfig))
                .build();
    }
}

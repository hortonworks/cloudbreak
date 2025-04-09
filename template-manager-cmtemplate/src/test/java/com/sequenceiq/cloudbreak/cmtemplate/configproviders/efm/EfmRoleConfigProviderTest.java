package com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class EfmRoleConfigProviderTest {

    private static final String CONNECTION_URL = "jdbc:postgresql://testhost:5432/efm";

    private static final String USER_NAME = "efm_server_user";

    private static final String USER_PASSWORD = "efm_server_password";

    private EfmRoleConfigProvider underTest;

    @BeforeEach
    public void setup() {
        underTest = new EfmRoleConfigProvider();
    }

    @Test
    public void testDbConfigs() {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/efm.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor);

        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(EfmRoles.EFM_SERVER, preparationObject);

        assertThat(roleConfigs).hasSameElementsAs(
            List.of(config("efm.db.url", CONNECTION_URL),
                config("efm.db.username", USER_NAME),
                config("efm.db.password", USER_PASSWORD),
                config("efm.db.driverClass", "org.postgresql.Driver")));
    }

    private TemplatePreparationObject getTemplatePreparationObject(CmTemplateProcessor cmTemplateProcessor) {
        BlueprintView blueprintView = new BlueprintView(null, null, null, null, cmTemplateProcessor);

        RdsConfigWithoutCluster rdsConfig = mock(RdsConfigWithoutCluster.class);
        when(rdsConfig.getType()).thenReturn(DatabaseType.EFM.toString());
        when(rdsConfig.getDatabaseEngine()).thenReturn(DatabaseVendor.POSTGRES);
        when(rdsConfig.getConnectionDriver()).thenReturn(DatabaseVendor.POSTGRES.connectionDriver());
        when(rdsConfig.getConnectionURL()).thenReturn(CONNECTION_URL);
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(USER_PASSWORD);

        return TemplatePreparationObject.Builder.builder()
            .withBlueprintView(blueprintView)
            .withRdsViews(Set.of(rdsConfig)
                .stream()
                .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", false))
                .collect(Collectors.toSet()))
            .build();
    }
}

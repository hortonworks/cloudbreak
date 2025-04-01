package com.sequenceiq.cloudbreak.cmtemplate.configproviders.das;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil;

@ExtendWith(MockitoExtension.class)
public class DasConfigProviderTest {

    private static final String HIVE_DAS = "HIVE_DAS";

    private static final String DB_PROVIDER = "postgres";

    private static final String HOST = "host";

    private static final String PORT = "12345";

    private static final String DB_NAME = "dbName";

    private static final String USER_NAME = "userName";

    private static final String PASSWORD = "password";

    private DasConfigProvider underTest;

    @Mock
    private CmTemplateProcessor mockTemplateProcessor;

    @BeforeEach
    public void setUp() {
        underTest = new DasConfigProvider();
    }

    @Test
    public void getServiceConfigs() {
        RdsConfigWithoutCluster rdsConfig = mock(RdsConfigWithoutCluster.class);
        when(rdsConfig.getType()).thenReturn(HIVE_DAS);
        when(rdsConfig.getConnectionURL()).thenReturn(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getSslMode()).thenReturn(RdsSslMode.DISABLED);

        TemplatePreparationObject tpo = new Builder()
                .withRdsViews(Set.of(rdsConfig)
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", false))
                        .collect(Collectors.toSet()))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        validateServiceConfigsNoDbSsl(result);
    }

    private ClouderaManagerRepo generateCmRepo(Versioned version) {
        return new ClouderaManagerRepo()
                .withBaseUrl("baseurl")
                .withGpgKeyUrl("gpgurl")
                .withPredefined(true)
                .withVersion(version.getVersion());
    }

    private void validateServiceConfigsNoDbSsl(List<ApiClusterTemplateConfig> result) {
        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        assertThat(configToValue).containsOnly(
                entry("data_analytics_studio_database_host", HOST),
                entry("data_analytics_studio_database_port", PORT),
                entry("data_analytics_studio_database_name", DB_NAME),
                entry("data_analytics_studio_database_username", USER_NAME),
                entry("data_analytics_studio_database_password", PASSWORD));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    @Test
    public void getServiceConfigsWhenGoodCmVersionButDbSslIsNotRequested() {
        RdsConfigWithoutCluster rdsConfig = mock(RdsConfigWithoutCluster.class);
        when(rdsConfig.getType()).thenReturn(HIVE_DAS);
        when(rdsConfig.getConnectionURL()).thenReturn(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getSslMode()).thenReturn(RdsSslMode.DISABLED);

        TemplatePreparationObject tpo = new Builder()
                .withRdsViews(Set.of(rdsConfig)
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", false))
                        .collect(Collectors.toSet()))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_9_2), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        validateServiceConfigsNoDbSsl(result);
    }

    @Test
    public void getServiceConfigsWhenDbSslVerifyFullSSLMode() {
        RdsConfigWithoutCluster rdsConfig = mock(RdsConfigWithoutCluster.class);
        when(rdsConfig.getType()).thenReturn(HIVE_DAS);
        when(rdsConfig.getConnectionURL()).thenReturn(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getSslMode()).thenReturn(RdsSslMode.ENABLED);

        TemplatePreparationObject tpo = new Builder()
                .withRdsViews(Set.of(rdsConfig)
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "/foo/cert.pem", "AWS", true))
                        .collect(Collectors.toSet()))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_9_2), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        assertThat(configToValue).containsOnly(
                entry("data_analytics_studio_database_host", HOST),
                entry("data_analytics_studio_database_port", PORT),
                entry("data_analytics_studio_database_name", DB_NAME),
                entry("data_analytics_studio_database_username", USER_NAME),
                entry("data_analytics_studio_database_password", PASSWORD),
                entry("data_analytics_studio_database_url_query_params", "?sslmode=verify-full&sslrootcert=/foo/cert.pem"));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    @Test
    public void getServiceConfigsWhenDbSslVerifyCaSSLMode() {
        RdsConfigWithoutCluster rdsConfig = mock(RdsConfigWithoutCluster.class);
        when(rdsConfig.getType()).thenReturn(HIVE_DAS);
        when(rdsConfig.getConnectionURL()).thenReturn(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getSslMode()).thenReturn(RdsSslMode.ENABLED);

        TemplatePreparationObject tpo = new Builder()
                .withRdsViews(Set.of(rdsConfig)
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "/foo/cert.pem", "GCP", true))
                        .collect(Collectors.toSet()))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_9_2), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        assertThat(configToValue).containsOnly(
                entry("data_analytics_studio_database_host", HOST),
                entry("data_analytics_studio_database_port", PORT),
                entry("data_analytics_studio_database_name", DB_NAME),
                entry("data_analytics_studio_database_username", USER_NAME),
                entry("data_analytics_studio_database_password", PASSWORD),
                entry("data_analytics_studio_database_url_query_params", "?sslmode=verify-ca&sslrootcert=/foo/cert.pem"));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    @Test
    public void getRoleConfigs() {
        TemplatePreparationObject tpo = new Builder().build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(DasRoles.WEBAPP, mockTemplateProcessor, tpo);

        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        assertThat(configToValue).containsOnly(
                entry("data_analytics_studio_user_authentication", "KNOX_PROXY"));

        result = underTest.getRoleConfigs(DasRoles.EVENTPROCESSOR, mockTemplateProcessor, tpo);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void getServiceType() {
        assertThat(underTest.getServiceType()).isEqualTo(DasRoles.DAS);
    }

    @Test
    public void getRoleTypes() {
        assertThat(underTest.getRoleTypes()).containsOnly(DasRoles.WEBAPP, DasRoles.EVENTPROCESSOR);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededTrue() {
        when(mockTemplateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(true);

        RdsConfigWithoutCluster rdsConfig = mock(RdsConfigWithoutCluster.class);

        when(rdsConfig.getType()).thenReturn(HIVE_DAS);
        when(rdsConfig.getConnectionURL()).thenReturn(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        TemplatePreparationObject tpo = new Builder()
                .withRdsViews(Set.of(rdsConfig)
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", false))
                        .collect(Collectors.toSet())
                ).build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);
        assertThat(result).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededFalseWhenNoDasOnCluster() {
        when(mockTemplateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(false);

        RdsConfigWithoutCluster rdsConfig = mock(RdsConfigWithoutCluster.class);

        when(rdsConfig.getType()).thenReturn(HIVE_DAS);
        when(rdsConfig.getConnectionURL()).thenReturn(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        TemplatePreparationObject tpo = new Builder()
                .withRdsViews(Set.of(rdsConfig)
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", false))
                        .collect(Collectors.toSet())
                ).build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);
        assertThat(result).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededFalseWhenNoDBRegistered() {
        TemplatePreparationObject tpo = new Builder().build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);
        assertThat(result).isFalse();
    }

}

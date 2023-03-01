package com.sequenceiq.cloudbreak.cmtemplate.configproviders.queryprocessor;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.views.RdsView;

public class QueryProcessorConfigProviderTest {

    private static final String QUERY_PROCESSOR = "QUERY_PROCESSOR";

    private static final String DB_PROVIDER = "postgres";

    private static final String HOST = "host";

    private static final String PORT = "12345";

    private static final String DB_NAME = "dbName";

    private static final String USER_NAME = "userName";

    private static final String PASSWORD = "password";

    private QueryProcessorConfigProvider underTest;

    @BeforeEach
    public void setUp() {
        underTest = new QueryProcessorConfigProvider();
    }

    @Test
    public void getServiceConfigs() {
        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(QUERY_PROCESSOR);
        when(rdsConfig.getConnectionURL()).thenReturn(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getHost()).thenReturn(HOST);
        when(rdsConfig.getDatabaseName()).thenReturn(DB_NAME);
        when(rdsConfig.getPort()).thenReturn(PORT);
        when(rdsConfig.getSubprotocol()).thenReturn(DB_PROVIDER);
        when(rdsConfig.isUseSsl()).thenReturn(false);
        TemplatePreparationObject tpo = new Builder()
                .withRdsViews(Set.of(rdsConfig))
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
                entry("query_processor_database_host", HOST),
                entry("query_processor_database_port", PORT),
                entry("query_processor_database_name", DB_NAME),
                entry("query_processor_database_username", USER_NAME),
                entry("query_processor_database_password", PASSWORD));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    @Test
    public void getServiceConfigsWhenGoodCmVersionButDbSslIsNotRequested() {
        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(QUERY_PROCESSOR);
        when(rdsConfig.getConnectionURL()).thenReturn(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getHost()).thenReturn(HOST);
        when(rdsConfig.getDatabaseName()).thenReturn(DB_NAME);
        when(rdsConfig.getPort()).thenReturn(PORT);
        when(rdsConfig.getSubprotocol()).thenReturn(DB_PROVIDER);
        when(rdsConfig.isUseSsl()).thenReturn(false);
        TemplatePreparationObject tpo = new Builder()
                .withRdsViews(Set.of(rdsConfig))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_9_2), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        validateServiceConfigsNoDbSsl(result);
    }

    @Test
    public void getServiceConfigsWhenDbSsl() {
        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(QUERY_PROCESSOR);
        when(rdsConfig.getConnectionURL()).thenReturn(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getHost()).thenReturn(HOST);
        when(rdsConfig.getDatabaseName()).thenReturn(DB_NAME);
        when(rdsConfig.getPort()).thenReturn(PORT);
        when(rdsConfig.getSubprotocol()).thenReturn(DB_PROVIDER);
        when(rdsConfig.isUseSsl()).thenReturn(true);
        when(rdsConfig.getConnectionURLOptions()).thenReturn("?option1=value1&option2=value2");
        TemplatePreparationObject tpo = new Builder()
                .withRdsViews(Set.of(rdsConfig))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_9_2), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        assertThat(configToValue).containsOnly(
                entry("query_processor_database_host", HOST),
                entry("query_processor_database_port", PORT),
                entry("query_processor_database_name", DB_NAME),
                entry("query_processor_database_username", USER_NAME),
                entry("query_processor_database_password", PASSWORD),
                entry("query_processor_database_url_query_params", "&option1=value1&option2=value2"));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    @ParameterizedTest(name = "connectionURLOptions={0}")
    @ValueSource(strings = {"", " ", "option1=value1&option2=value2"})
    public void getServiceConfigsWhenDbSslAndMalformedOptions(String connectionURLOptions) {
        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(QUERY_PROCESSOR);
        when(rdsConfig.getConnectionURL()).thenReturn(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getHost()).thenReturn(HOST);
        when(rdsConfig.getDatabaseName()).thenReturn(DB_NAME);
        when(rdsConfig.getPort()).thenReturn(PORT);
        when(rdsConfig.getSubprotocol()).thenReturn(DB_PROVIDER);
        when(rdsConfig.isUseSsl()).thenReturn(true);
        when(rdsConfig.getConnectionURLOptions()).thenReturn(connectionURLOptions);
        TemplatePreparationObject tpo = new Builder()
                .withRdsViews(Set.of(rdsConfig))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_9_2), null)
                .build();

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.getServiceConfigs(null, tpo));

        assertThat(illegalStateException).hasMessage(
                String.format("Malformed connectionURLOptions string; expected to start with '?' but it did not. Received: '%s'", connectionURLOptions));
    }

    @Test
    public void getServiceType() {
        assertThat(underTest.getServiceType()).isEqualTo(QueryStoreRoles.QUERY_PROCESSOR);
    }

    @Test
    public void getRoleTypes() {
        assertThat(underTest.getRoleTypes()).containsOnly(QueryStoreRoles.QUERY_PROCESSOR);
    }

    @Test
    public void isConfigurationNeededTrue() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        when(mockTemplateProcessor.isRoleTypePresentInService(QueryStoreRoles.QUERY_PROCESSOR, List.of(QueryStoreRoles.QUERY_PROCESSOR))).thenReturn(true);

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(QUERY_PROCESSOR);
        when(rdsConfig.getConnectionURL()).thenReturn(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        TemplatePreparationObject tpo = new Builder().withRdsViews(Set.of(rdsConfig)).build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);
        assertThat(result).isTrue();
    }

    @Test
    public void isConfigurationNeededFalseWhenNoHueQueryProcessorOnCluster() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        when(mockTemplateProcessor.isRoleTypePresentInService(QueryStoreRoles.QUERY_PROCESSOR, List.of(QueryStoreRoles.QUERY_PROCESSOR))).thenReturn(false);

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(QUERY_PROCESSOR);
        when(rdsConfig.getConnectionURL()).thenReturn(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        TemplatePreparationObject tpo = new Builder().withRdsViews(Set.of(rdsConfig)).build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);
        assertThat(result).isFalse();
    }

    @Test
    public void isConfigurationNeededFalseWhenNoDBRegistered() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        when(mockTemplateProcessor.isRoleTypePresentInService(QueryStoreRoles.QUERY_PROCESSOR, List.of(QueryStoreRoles.QUERY_PROCESSOR))).thenReturn(true);

        TemplatePreparationObject tpo = new Builder().build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);
        assertThat(result).isFalse();
    }

}

package com.sequenceiq.cloudbreak.cmtemplate.configproviders.queryprocessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
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
        TemplatePreparationObject tpo = new Builder().withRdsViews(Set.of(rdsConfig)).build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);
        Map<String, String> configToValue =
                result.stream().collect(Collectors.toMap(ApiClusterTemplateConfig::getName, ApiClusterTemplateConfig::getValue));
        assertThat(configToValue).containsOnly(
                new SimpleEntry<>("query_processor_database_host", HOST),
                new SimpleEntry<>("query_processor_database_port", PORT),
                new SimpleEntry<>("query_processor_database_name", DB_NAME),
                new SimpleEntry<>("query_processor_database_username", USER_NAME),
                new SimpleEntry<>("query_processor_database_password", PASSWORD));
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
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededFalseWhenNoHueQueryProcessorOnClusterr() {
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
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededFalseWhenNoDBRegistered() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        when(mockTemplateProcessor.isRoleTypePresentInService(QueryStoreRoles.QUERY_PROCESSOR, List.of(QueryStoreRoles.QUERY_PROCESSOR))).thenReturn(true);

        TemplatePreparationObject tpo = new Builder().build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);
        assertThat(result).isFalse();
    }
}

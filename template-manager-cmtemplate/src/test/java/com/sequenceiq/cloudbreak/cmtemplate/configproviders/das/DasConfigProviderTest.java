package com.sequenceiq.cloudbreak.cmtemplate.configproviders.das;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;

public class DasConfigProviderTest {

    private static final String HIVE_DAS = "HIVE_DAS";

    private static final String DB_PROVIDER = "postgres";

    private static final String HOST = "host";

    private static final String PORT = "12345";

    private static final String DB_NAME = "dbName";

    private static final String USER_NAME = "userName";

    private static final String PASSWORD = "password";

    private DasConfigProvider underTest;

    @Before
    public void setUp() {
        underTest = new DasConfigProvider();
    }

    @Test
    public void getServiceConfigs() {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(HIVE_DAS);
        rdsConfig.setConnectionURL(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        rdsConfig.setConnectionUserName(USER_NAME);
        rdsConfig.setConnectionPassword(PASSWORD);
        TemplatePreparationObject tpo = new Builder().withRdsConfigs(Set.of(rdsConfig)).build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);
        Map<String, String> paramToVariable =
                result.stream().collect(Collectors.toMap(ApiClusterTemplateConfig::getName, ApiClusterTemplateConfig::getValue));
        assertThat(paramToVariable).containsOnly(
            new SimpleEntry<>("data_analytics_studio_database_host", HOST),
            new SimpleEntry<>("data_analytics_studio_database_port", PORT),
            new SimpleEntry<>("data_analytics_studio_database_name", DB_NAME),
            new SimpleEntry<>("data_analytics_studio_database_username", USER_NAME),
            new SimpleEntry<>("data_analytics_studio_database_password", PASSWORD));
    }

    @Test
    public void getRoleConfigs() {
        TemplatePreparationObject tpo = new Builder().build();
        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(DasRoles.WEBAPP, tpo);
        Map<String, String> paramToVariable =
                result.stream().collect(Collectors.toMap(ApiClusterTemplateConfig::getName, ApiClusterTemplateConfig::getValue));
        assertThat(paramToVariable).containsOnly(
            new SimpleEntry<>("data_analytics_studio_user_authentication", "KNOX_PROXY"));
        result = underTest.getRoleConfigs(DasRoles.EVENTPROCESSOR, tpo);
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
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        when(mockTemplateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(true);

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(HIVE_DAS);
        rdsConfig.setConnectionURL(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        rdsConfig.setConnectionUserName(USER_NAME);
        rdsConfig.setConnectionPassword(PASSWORD);
        TemplatePreparationObject tpo = new Builder().withRdsConfigs(Set.of(rdsConfig)).build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);
        assertThat(result).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededFalseWhenNoDasOnClusterr() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        when(mockTemplateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(false);

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(HIVE_DAS);
        rdsConfig.setConnectionURL(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        rdsConfig.setConnectionUserName(USER_NAME);
        rdsConfig.setConnectionPassword(PASSWORD);
        TemplatePreparationObject tpo = new Builder().withRdsConfigs(Set.of(rdsConfig)).build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);
        assertThat(result).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededFalseWhenNoDBRegistered() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        when(mockTemplateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(true);

        TemplatePreparationObject tpo = new Builder().build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);
        assertThat(result).isFalse();
    }
}

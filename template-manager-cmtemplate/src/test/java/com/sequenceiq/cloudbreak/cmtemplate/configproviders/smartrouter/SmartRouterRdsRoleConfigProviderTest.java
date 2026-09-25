package com.sequenceiq.cloudbreak.cmtemplate.configproviders.smartrouter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@ExtendWith(MockitoExtension.class)
class SmartRouterRdsRoleConfigProviderTest {

    private static final String SERVICE_TYPE = "SMARTROUTER";

    private static final String ROLE_TYPE = "SMARTROUTER_SERVER";

    private static final String CERTIFICATE_PATH = "/path/to/cert.pem";

    private SmartRouterRdsRoleConfigProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new SmartRouterRdsRoleConfigProvider();
    }

    @Test
    void testDbType() {
        assertThat(underTest.dbType()).isEqualTo(DatabaseType.SMART_ROUTER);
    }

    @Test
    void testDbUserKey() {
        assertThat(underTest.dbUserKey()).isEqualTo("smartrouter_database_user");
    }

    @Test
    void testDbPasswordKey() {
        assertThat(underTest.dbPasswordKey()).isEqualTo("smartrouter_database_password");
    }

    @Test
    void testGetServiceType() {
        assertThat(underTest.getServiceType()).isEqualTo(SERVICE_TYPE);
    }

    @Test
    void testGetRoleTypes() {
        assertThat(underTest.getRoleTypes()).containsExactly(ROLE_TYPE);
    }

    @Test
    void testGetRoleConfigsShouldBeEmptyBecauseEveryConfigIsOnServiceLevel() {
        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(ROLE_TYPE, mock(CmTemplateProcessor.class), mock(TemplatePreparationObject.class));

        assertThat(result).isEmpty();
    }

    @Test
    void testGetServiceConfigsShouldReturnTheDatabaseConnectionProperties() {
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        RdsView rdsView = mock(RdsView.class);
        when(source.getRdsView(DatabaseType.SMART_ROUTER)).thenReturn(rdsView);
        when(rdsView.getHost()).thenReturn("myhost");
        when(rdsView.getPort()).thenReturn("5432");
        when(rdsView.getDatabaseName()).thenReturn("smartrouter");
        when(rdsView.getSubprotocol()).thenReturn("postgresql");
        when(rdsView.getConnectionUserName()).thenReturn("user");
        when(rdsView.getConnectionPassword()).thenReturn("pass");

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(mock(CmTemplateProcessor.class), source);

        assertThat(result).hasSize(6);
        assertConfig(result, "smartrouter_database_host", "myhost");
        assertConfig(result, "smartrouter_database_port", "5432");
        assertConfig(result, "smartrouter_database_name", "smartrouter");
        assertConfig(result, "smartrouter_database_type", "postgresql");
        assertConfig(result, "smartrouter_database_user", "user");
        assertConfig(result, "smartrouter_database_password", "pass");
    }

    @Test
    void testGetServiceConfigsShouldPropagateTheSslCertificatePathToTheRdsView() {
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        RdsView rdsView = mock(RdsView.class);
        when(source.getRdsView(DatabaseType.SMART_ROUTER)).thenReturn(rdsView);
        when(source.getRdsSslCertificateFilePath()).thenReturn(CERTIFICATE_PATH);

        underTest.getServiceConfigs(mock(CmTemplateProcessor.class), source);

        verify(rdsView).setSslCertificateFilePath(CERTIFICATE_PATH);
    }

    @Test
    void testIsConfigurationNeededWhenTheRdsViewAndTheRoleTypeArePresent() {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        when(source.getRdsView(DatabaseType.SMART_ROUTER)).thenReturn(mock(RdsView.class));
        when(cmTemplateProcessor.isRoleTypePresentInService(SERVICE_TYPE, List.of(ROLE_TYPE))).thenReturn(true);

        assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, source)).isTrue();
    }

    @Test
    void testIsConfigurationNeededWhenThereIsNoRdsView() {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        when(source.getRdsView(DatabaseType.SMART_ROUTER)).thenReturn(null);

        assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, source)).isFalse();
    }

    @Test
    void testIsConfigurationNeededWhenTheRoleTypeIsNotPresentInTheService() {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        when(source.getRdsView(DatabaseType.SMART_ROUTER)).thenReturn(mock(RdsView.class));
        when(cmTemplateProcessor.isRoleTypePresentInService(SERVICE_TYPE, List.of(ROLE_TYPE))).thenReturn(false);

        assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, source)).isFalse();
    }

    private void assertConfig(List<ApiClusterTemplateConfig> configs, String name, String value) {
        assertThat(configs).anyMatch(config -> name.equals(config.getName()) && value.equals(config.getValue()));
    }
}

package com.sequenceiq.cloudbreak.cmtemplate.configproviders.dataviz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@ExtendWith(MockitoExtension.class)
class DatavizRoleConfigProviderTest {

    @InjectMocks
    private DatavizRoleConfigProvider underTest;

    @Mock
    private TemplatePreparationObject templatePreparationObject;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private RdsView rdsView;

    @Test
    void testDbUserKey() {
        assertEquals(DatavizRoleConfigProvider.METASTORE_DB_NAME, underTest.dbUserKey());
    }

    @Test
    void testDbPasswordKey() {
        assertEquals(DatavizRoleConfigProvider.METASTORE_DB_PASSWORD, underTest.dbPasswordKey());
    }

    @Test
    void testGetServiceType() {
        assertEquals(DatavizRoles.DATAVIZ, underTest.getServiceType());
    }

    @Test
    void testGetRoleTypes() {
        assertEquals(List.of(DatavizRoles.DATAVIZ_WEBSERVER), underTest.getRoleTypes());
    }

    @Test
    void testDbType() {
        assertEquals(DatabaseType.DATAVIZ, underTest.dbType());
    }

    @Test
    void testGetRoleConfigsForDatavizWithoutSSL() {
        when(rdsView.getHost()).thenReturn("localhost");
        when(rdsView.getPort()).thenReturn("5432");
        when(rdsView.getDatabaseName()).thenReturn("dataviz_db");
        when(rdsView.getUserName()).thenReturn("dataviz_user");
        when(rdsView.getPassword()).thenReturn("secret");
        when(rdsView.isUseSsl()).thenReturn(false);

        List<ApiClusterTemplateConfig> result = null;

        try (MockedStatic<ConfigUtils> utilities = Mockito.mockStatic(ConfigUtils.class)) {
            utilities.when(() -> ConfigUtils.getRdsViewOfType(any(), any())).thenReturn(rdsView);
            utilities.when(() -> ConfigUtils.config(any(), any())).thenCallRealMethod();

            result = underTest.getServiceConfigs(cmTemplateProcessor, templatePreparationObject);
        }

        assertEquals(6, result.size());
        assertEquals(DatavizRoleConfigProvider.METADATA_STORE, result.get(0).getName());
        assertEquals("postgresql", result.get(0).getValue());
        assertEquals("localhost", result.get(1).getValue());
        assertEquals("5432", result.get(2).getValue());
        assertEquals("dataviz_db", result.get(3).getValue());
        assertEquals("dataviz_user", result.get(4).getValue());
        assertEquals("secret", result.get(5).getValue());
    }

    @Test
    //CHECKSTYLE:OFF
    void testGetRoleConfigsForDatavizWithSSL() {
        when(rdsView.getHost()).thenReturn("remotehost");
        when(rdsView.getPort()).thenReturn("6543");
        when(rdsView.getDatabaseName()).thenReturn("secure_db");
        when(rdsView.getUserName()).thenReturn("secure_user");
        when(rdsView.getPassword()).thenReturn("topsecret");
        when(rdsView.isUseSsl()).thenReturn(true);
        when(rdsView.getSslCertificateFilePath()).thenReturn("/etc/ssl/certs/ca.crt");

        List<ApiClusterTemplateConfig> result = null;
        try (MockedStatic<ConfigUtils> utilities = Mockito.mockStatic(ConfigUtils.class)) {
            utilities.when(() -> ConfigUtils.getRdsViewOfType(any(), any())).thenReturn(rdsView);
            utilities.when(() -> ConfigUtils.config(any(), any())).thenCallRealMethod();
            utilities.when(() -> ConfigUtils.getSafetyValveProperty(any(), any())).thenCallRealMethod();

            result = underTest.getServiceConfigs(cmTemplateProcessor, templatePreparationObject);
        }

        assertEquals(7, result.size());
        ApiClusterTemplateConfig sslConfig = result.get(6);
        assertEquals(DatavizRoleConfigProvider.DATAVIZ_SAFETY_VALVE, sslConfig.getName());
        assertEquals("<property><name>DATAVIZ_ADVANCED_SETTINGS</name><value>\n" +
                        "if &apos;OPTIONS&apos; not in DATABASES[&apos;default&apos;]:\n" +
                        "\tDATABASES[&apos;default&apos;][&apos;OPTIONS&apos;] = {}\n" +
                        "DATABASES[&apos;default&apos;][&apos;OPTIONS&apos;].update({\n" +
                        "\t&apos;sslmode&apos;: &apos;verify-ca&apos;,\n" +
                        "\t&apos;sslrootcert&apos;: &apos;/etc/ssl/certs/ca.crt&apos;\n" +
                        "})</value></property>",
                sslConfig.getValue());
    }
    //CHECKSTYLE:ON

    @Test
    void testGetRoleConfigsForOtherRoleReturnsEmptyList() {
        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs("OTHER_ROLE", cmTemplateProcessor, templatePreparationObject);
        assertTrue(result.isEmpty());
    }
}
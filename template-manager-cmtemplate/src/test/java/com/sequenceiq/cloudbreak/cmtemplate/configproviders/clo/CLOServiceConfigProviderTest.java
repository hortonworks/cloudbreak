package com.sequenceiq.cloudbreak.cmtemplate.configproviders.clo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.ProductDetailsView;
import com.sequenceiq.cloudbreak.template.views.RdsView;

class CLOServiceConfigProviderTest {
    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @Mock
    private GeneralClusterConfigs generalClusterConfigs;

    @Mock
    private ProductDetailsView productDetailsView;

    @Mock
    private ClouderaManagerRepo cmRepo;

    private AutoCloseable closeable;

    @InjectMocks
    private CLOServiceConfigProvider cloServiceConfigProvider;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        when(source.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);
        lenient().when(source.getProductDetailsView()).thenReturn(productDetailsView);
        lenient().when(productDetailsView.getCm()).thenReturn(cmRepo);
        lenient().when(cmRepo.getVersion()).thenReturn(null);
        lenient().when(cmTemplateProcessor.getComponentsByHostGroup()).thenReturn(Map.of());
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    void testGetServiceType() {
        assertEquals(CLOServiceRoles.CLO_SERVICE, cloServiceConfigProvider.getServiceType());
    }

    @Test
    void testGetRoleTypes() {
        assertEquals(List.of(CLOServiceRoles.CLO_SERVER), cloServiceConfigProvider.getRoleTypes());
    }

    @Test
    void testIsConfigurationNeeded() {
        assertTrue(cloServiceConfigProvider.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @Test
    void testGetServiceConfigs() {
        String resourceCrn = "test-resource-crn";
        String accountId = "test-account-id";
        String environmentCrn = "test-environment-crn";
        String cloudProvider = "AWS";

        when(generalClusterConfigs.getResourceCrn()).thenReturn(resourceCrn);
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.of(accountId));
        when(generalClusterConfigs.getEnvironmentCrn()).thenReturn(environmentCrn);
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);

        List<ApiClusterTemplateConfig> configs = cloServiceConfigProvider.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(4, configs.size());
        assertEquals(CLOServiceConfigProvider.CLO_DATAHUB_ENVIRONMENT_CRN, configs.getFirst().getName());
        assertEquals(environmentCrn, configs.getFirst().getValue());

        assertEquals(CLOServiceConfigProvider.CLO_DATAHUB_RESOURCE_CRN, configs.get(1).getName());
        assertEquals(resourceCrn, configs.get(1).getValue());

        assertEquals(CLOServiceConfigProvider.CLO_ACCOUNT_ID, configs.get(2).getName());
        assertEquals(accountId, configs.get(2).getValue());

        assertEquals(CLOServiceConfigProvider.CLO_CLOUD_PROVIDER, configs.get(3).getName());
        assertEquals(cloudProvider, configs.get(3).getValue());
    }

    @Test
    void testGetServiceConfigsWithPostgresRds() {
        String resourceCrn = "test-resource-crn";
        String accountId = "test-account-id";
        String environmentCrn = "test-environment-crn";
        String jdbcUrl = "jdbc:postgresql://host:5432/clo";
        String dbUser = "clouser";
        String dbPassword = "clopass";

        when(generalClusterConfigs.getResourceCrn()).thenReturn(resourceCrn);
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.of(accountId));
        when(generalClusterConfigs.getEnvironmentCrn()).thenReturn(environmentCrn);
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(cmRepo.getVersion()).thenReturn("7.13.2.20000");

        RdsView lakehouseOptimizerRds = new RdsView();
        lakehouseOptimizerRds.setConnectionURL(jdbcUrl);
        lakehouseOptimizerRds.setHost("host");
        lakehouseOptimizerRds.setPort("5432");
        lakehouseOptimizerRds.setDatabaseName("clo");
        lakehouseOptimizerRds.setDatabaseVendor(DatabaseVendor.POSTGRES);
        lakehouseOptimizerRds.setConnectionUserName(dbUser);
        lakehouseOptimizerRds.setConnectionPassword(dbPassword);
        when(source.getRdsView(DatabaseType.LAKEHOUSE_OPTIMIZER)).thenReturn(lakehouseOptimizerRds);
        when(source.getRdsSslCertificateFilePath()).thenReturn("/tmp/rds-ca.pem");

        List<ApiClusterTemplateConfig> configs = cloServiceConfigProvider.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(11, configs.size());
        assertEquals(CLOServiceConfigProvider.CLO_DB_TYPE, configs.get(4).getName());
        assertEquals("postgresql", configs.get(4).getValue());
        assertEquals(CLOServiceConfigProvider.CLO_DB_HOST, configs.get(5).getName());
        assertEquals("host", configs.get(5).getValue());
        assertEquals(CLOServiceConfigProvider.CLO_DB_PORT, configs.get(6).getName());
        assertEquals("5432", configs.get(6).getValue());
        assertEquals(CLOServiceConfigProvider.CLO_DB_NAME, configs.get(7).getName());
        assertEquals("clo", configs.get(7).getValue());
        assertEquals(CLOServiceConfigProvider.CLO_DB_JDBC_URL_OVERRIDE, configs.get(8).getName());
        assertEquals(jdbcUrl, configs.get(8).getValue());
        assertEquals(CLOServiceConfigProvider.CLO_DB_USER, configs.get(9).getName());
        assertEquals(dbUser, configs.get(9).getValue());
        assertEquals(CLOServiceConfigProvider.CLO_DB_PASSWORD, configs.get(10).getName());
        assertEquals(dbPassword, configs.get(10).getValue());
    }

    @Test
    void testGetServiceConfigsWithSslRdsUsesFullJdbcUrl() {
        when(generalClusterConfigs.getResourceCrn()).thenReturn("crn");
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.of("acct"));
        when(generalClusterConfigs.getEnvironmentCrn()).thenReturn("env");
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(cmRepo.getVersion()).thenReturn("7.13.2.20000");

        String sslJdbcUrl = "jdbc:postgresql://host:5432/clo?sslmode=verify-full&sslrootcert=/tmp/rds-ca.pem";
        RdsView lakehouseOptimizerRds = new RdsView();
        lakehouseOptimizerRds.setConnectionURL(sslJdbcUrl);
        lakehouseOptimizerRds.setHost("host");
        lakehouseOptimizerRds.setPort("5432");
        lakehouseOptimizerRds.setDatabaseName("clo");
        lakehouseOptimizerRds.setDatabaseVendor(DatabaseVendor.POSTGRES);
        lakehouseOptimizerRds.setConnectionUserName("user");
        lakehouseOptimizerRds.setConnectionPassword("pass");
        when(source.getRdsView(DatabaseType.LAKEHOUSE_OPTIMIZER)).thenReturn(lakehouseOptimizerRds);
        when(source.getRdsSslCertificateFilePath()).thenReturn("/tmp/rds-ca.pem");

        List<ApiClusterTemplateConfig> configs = cloServiceConfigProvider.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(11, configs.size());
        assertEquals(CLOServiceConfigProvider.CLO_DB_JDBC_URL_OVERRIDE, configs.get(8).getName());
        assertEquals(sslJdbcUrl, configs.get(8).getValue());
    }

    @Test
    void testGetServiceConfigsWithMissingPortDefaultsTo5432() {
        when(generalClusterConfigs.getResourceCrn()).thenReturn("crn");
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.of("acct"));
        when(generalClusterConfigs.getEnvironmentCrn()).thenReturn("env");
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(cmRepo.getVersion()).thenReturn("7.13.2.20000");

        RdsView lakehouseOptimizerRds = new RdsView();
        lakehouseOptimizerRds.setConnectionURL("jdbc:postgresql://host/clo");
        lakehouseOptimizerRds.setHost("host");
        lakehouseOptimizerRds.setPort(null);
        lakehouseOptimizerRds.setDatabaseName("clo");
        lakehouseOptimizerRds.setDatabaseVendor(DatabaseVendor.POSTGRES);
        lakehouseOptimizerRds.setConnectionUserName("u");
        lakehouseOptimizerRds.setConnectionPassword("p");
        when(source.getRdsView(DatabaseType.LAKEHOUSE_OPTIMIZER)).thenReturn(lakehouseOptimizerRds);
        when(source.getRdsSslCertificateFilePath()).thenReturn("");

        List<ApiClusterTemplateConfig> configs = cloServiceConfigProvider.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(11, configs.size());
        assertEquals("postgresql", configs.get(4).getValue());
        assertEquals("5432", configs.get(6).getValue());
    }

    @Test
    void testGetServiceConfigsWithHaEnabled() {
        when(generalClusterConfigs.getResourceCrn()).thenReturn("crn");
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.of("acct"));
        when(generalClusterConfigs.getEnvironmentCrn()).thenReturn("env");
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(cmRepo.getVersion()).thenReturn("7.13.2.20000");
        when(cmTemplateProcessor.getComponentsByHostGroup()).thenReturn(Map.of(
                "master", Set.of(CLOServiceRoles.CLO_SERVER, "HDFS_NAMENODE"),
                "clostandby", Set.of(CLOServiceRoles.CLO_SERVER)
        ));

        List<ApiClusterTemplateConfig> configs = cloServiceConfigProvider.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(5, configs.size());
        assertTrue(configs.stream().anyMatch(c -> CLOServiceConfigProvider.CLO_HA_ENABLED.equals(c.getName())
                && "true".equals(c.getValue())));
    }

    @Test
    void testGetServiceConfigsHaDisabledWhenCmVersionTooOld() {
        when(generalClusterConfigs.getResourceCrn()).thenReturn("crn");
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.of("acct"));
        when(generalClusterConfigs.getEnvironmentCrn()).thenReturn("env");
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(cmRepo.getVersion()).thenReturn("7.13.1.0");
        when(cmTemplateProcessor.getComponentsByHostGroup()).thenReturn(Map.of(
                "master", Set.of(CLOServiceRoles.CLO_SERVER),
                "clostandby", Set.of(CLOServiceRoles.CLO_SERVER)
        ));

        List<ApiClusterTemplateConfig> configs = cloServiceConfigProvider.getServiceConfigs(cmTemplateProcessor, source);

        assertFalse(configs.stream().anyMatch(c -> CLOServiceConfigProvider.CLO_HA_ENABLED.equals(c.getName())));
    }

    @Test
    void testGetServiceConfigsHaDisabledWhenSingleCloServer() {
        when(generalClusterConfigs.getResourceCrn()).thenReturn("crn");
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.of("acct"));
        when(generalClusterConfigs.getEnvironmentCrn()).thenReturn("env");
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(cmRepo.getVersion()).thenReturn("7.13.2.10000");
        when(cmTemplateProcessor.getComponentsByHostGroup()).thenReturn(Map.of(
                "master", Set.of(CLOServiceRoles.CLO_SERVER, "HDFS_NAMENODE")
        ));

        List<ApiClusterTemplateConfig> configs = cloServiceConfigProvider.getServiceConfigs(cmTemplateProcessor, source);

        assertFalse(configs.stream().anyMatch(c -> CLOServiceConfigProvider.CLO_HA_ENABLED.equals(c.getName())));
    }

    @Test
    void testGetServiceConfigsWhenAccountIdIsEmpty() {
        String resourceCrn = "test-resource-crn";

        when(generalClusterConfigs.getResourceCrn()).thenReturn(resourceCrn);
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.empty());
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);

        List<ApiClusterTemplateConfig> configs = cloServiceConfigProvider.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(4, configs.size());
        assertEquals("UNKNOWN", configs.get(2).getValue());
    }
}

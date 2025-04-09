package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ssb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.flink.FlinkConfigProviderUtils;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class SqlStreamBuilderSseConfigProviderTest {

    private static final String CONNECTION_URL = "jdbc:postgresql://cluster-master0.bar.com:5432/ssb_admin";

    private static final String CONNECTION_URL_WITH_SSL = "jdbc:postgresql://cluster-master0.bar.com:5432/ssb_admin" +
            "?sslmode=verify-full&sslrootcert=/foo/cert.pem";

    @Mock
    private FlinkConfigProviderUtils utils;

    @InjectMocks
    private SqlStreamBuilderSseConfigProvider underTest;

    @Test
    public void testNoConfigNeeded() {
        CmTemplateProcessor cmTemplateProcessor = initTemplateProcessor("7.2.10");
        TemplatePreparationObject preparationObject = initTemplatePreparationObject(cmTemplateProcessor);

        assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject)).isFalse();
    }

    @Test
    public void testConfigNeeded() {
        CmTemplateProcessor cmTemplateProcessor = initTemplateProcessor("7.2.11");
        TemplatePreparationObject preparationObject = initTemplatePreparationObject(cmTemplateProcessor);

        assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject)).isTrue();
    }

    @Test
    public void testProperDbConfig() {
        CmTemplateProcessor cmTemplateProcessor = initTemplateProcessor("7.2.11");
        TemplatePreparationObject preparationObject = initTemplatePreparationObject(cmTemplateProcessor);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        validateServiceConfigsNoDbSsl(serviceConfigs);
    }

    @Test
    public void getServiceConfigsWhenGoodFlinkVersionButDbSslIsNotRequested() {
        CmTemplateProcessor cmTemplateProcessor = initTemplateProcessor("7.2.11");
        TemplatePreparationObject preparationObject = initTemplatePreparationObject(cmTemplateProcessor, CMRepositoryVersionUtil.FLINK_VERSION_1_15_1,
                false);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        validateServiceConfigsNoDbSsl(serviceConfigs);
    }

    @Test
    public void getServiceConfigsWhenDbSslIsRequestedButBadFlinkVersion() {
        CmTemplateProcessor cmTemplateProcessor = initTemplateProcessor("7.2.11");
        TemplatePreparationObject preparationObject = initTemplatePreparationObject(cmTemplateProcessor, () -> "1.14.0", true);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        validateServiceConfigsNoDbSsl(serviceConfigs);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"1.15.1", "1.15.1-csadh1.9.0.1-cdh7.2.16.0-254-37351973", "1.15.2", "1.16.0", "1.19.1-csa1.14.0.0-12345678"})
    public void getServiceConfigsWhenDbSsl(String flinkVersion) {
        CmTemplateProcessor cmTemplateProcessor = initTemplateProcessor("7.2.11");
        TemplatePreparationObject preparationObject = initTemplatePreparationObject(cmTemplateProcessor, () -> flinkVersion, true);

        List<ClouderaManagerProduct> products = preparationObject.getProductDetailsView().getProducts();
        when(utils.getFlinkProduct(products)).thenReturn(Optional.ofNullable(products.getFirst()));

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        Map<String, String> actualConfig = ConfigTestUtil.getConfigNameToValueMap(serviceConfigs);
        Map<String, String> expectedConfig = getNoDbSslConfig();
        expectedConfig.put(SqlStreamBuilderSseConfigProvider.DATABASE_JDBC_URL_OVERRIDE, CONNECTION_URL_WITH_SSL);

        assertThat(actualConfig).containsExactlyInAnyOrderEntriesOf(expectedConfig);
    }

    @Test
    public void testAddReleaseNameConfigCalled() {
        CmTemplateProcessor cmTemplateProcessor = initTemplateProcessor("7.3.1");
        TemplatePreparationObject preparationObject = initTemplatePreparationObject(cmTemplateProcessor, () -> "1.14.0", false);

        List<ClouderaManagerProduct> products = preparationObject.getProductDetailsView().getProducts();
        Optional<ClouderaManagerProduct> flinkProduct = Optional.of(products.getFirst());
        when(utils.getFlinkProduct(products)).thenReturn(flinkProduct);

        underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        verify(utils, times(1)).addReleaseNameIfNeeded(eq("7.3.1"), anyList(), eq(flinkProduct));
    }

    private void validateServiceConfigsNoDbSsl(List<ApiClusterTemplateConfig> serviceConfigs) {
        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(serviceConfigs);
        Map<String, String> expectedConfig = getNoDbSslConfig();

        assertThat(configToValue).containsExactlyInAnyOrderEntriesOf(expectedConfig);
    }

    private Map<String, String> getNoDbSslConfig() {
        return new HashMap<>(Map.of(
                SqlStreamBuilderSseConfigProvider.DATABASE_TYPE, "postgresql",
                SqlStreamBuilderSseConfigProvider.DATABASE_HOST, "testhost",
                SqlStreamBuilderSseConfigProvider.DATABASE_PORT, "5432",
                SqlStreamBuilderSseConfigProvider.DATABASE_SCHEMA, "ssb_admin",
                SqlStreamBuilderSseConfigProvider.DATABASE_USER, "ssb_test_user",
                SqlStreamBuilderSseConfigProvider.DATABASE_PASSWORD, "ssb_test_pw"
        ));
    }

    private CmTemplateProcessor initTemplateProcessor(String cdhVersion) {
        String json = FileReaderUtils.readFileFromClasspathQuietly("input/ssb.bp");
        json = json.replace("__CDH_VERSION__", cdhVersion);

        return new CmTemplateProcessor(json);
    }

    private TemplatePreparationObject initTemplatePreparationObject(CmTemplateProcessor cmTemplateProcessor) {
        return initTemplatePreparationObject(cmTemplateProcessor, null, false);
    }

    private TemplatePreparationObject initTemplatePreparationObject(CmTemplateProcessor cmTemplateProcessor, Versioned flinkVersion, boolean useDbSsl) {
        HostgroupView manager = new HostgroupView("manager", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.CORE, 2);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(null, null, null, null, cmTemplateProcessor);

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(DatabaseType.SQL_STREAM_BUILDER_ADMIN.toString());
        lenient().when(rdsConfig.getDatabaseVendor()).thenReturn(DatabaseVendor.POSTGRES);
        lenient().when(rdsConfig.getHost()).thenReturn("testhost");
        lenient().when(rdsConfig.getPort()).thenReturn("5432");
        lenient().when(rdsConfig.getDatabaseName()).thenReturn("ssb_admin");
        lenient().when(rdsConfig.getConnectionUserName()).thenReturn("ssb_test_user");
        lenient().when(rdsConfig.getConnectionPassword()).thenReturn("ssb_test_pw");
        lenient().when(rdsConfig.isUseSsl()).thenReturn(useDbSsl);
        lenient().when(rdsConfig.getConnectionURL()).thenReturn(useDbSsl ? CONNECTION_URL_WITH_SSL : CONNECTION_URL);

        return TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withHostgroupViews(Set.of(manager, master, worker))
                .withRdsViews(Set.of(rdsConfig))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), generateProducts(flinkVersion))
                .build();
    }

    private ClouderaManagerRepo generateCmRepo(Versioned version) {
        return new ClouderaManagerRepo()
                .withBaseUrl("baseurl")
                .withGpgKeyUrl("gpgurl")
                .withPredefined(true)
                .withVersion(version.getVersion());
    }

    private List<ClouderaManagerProduct> generateProducts(Versioned flinkVersion) {
        List<ClouderaManagerProduct> products = new ArrayList<>();

        if (flinkVersion != null) {
            products.add(new ClouderaManagerProduct()
                    .withName("FLINK")
                    .withVersion(flinkVersion.getVersion()));
        }

        return products;
    }

}

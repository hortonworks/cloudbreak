package com.sequenceiq.cloudbreak.cmtemplate.configproviders.atlas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase.HbaseRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.ProductDetailsView;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@ExtendWith(MockitoExtension.class)
class AtlasRdsRoleConfigProviderTest {

    private AtlasRdsRoleConfigProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new AtlasRdsRoleConfigProvider();
    }

    @Test
    void testDbType() {
        assertThat(underTest.dbType()).isEqualTo(DatabaseType.ATLAS);
    }

    @Test
    void testDbUserKey() {
        assertThat(underTest.dbUserKey()).isEqualTo("atlas_database_user");
    }

    @Test
    void testDbPasswordKey() {
        assertThat(underTest.dbPasswordKey()).isEqualTo("atlas_database_password");
    }

    @Test
    void testGetServiceType() {
        assertThat(underTest.getServiceType()).isEqualTo("ATLAS");
    }

    @Test
    void testGetRoleTypes() {
        assertThat(underTest.getRoleTypes()).containsExactly("ATLAS_SERVER");
    }

    @Test
    void testGetRoleConfigs() {
        assertThat(underTest.getRoleConfigs("ATLAS_SERVER", mock(CmTemplateProcessor.class), mock(TemplatePreparationObject.class))).isEmpty();
    }

    @Test
    void testIsConfigurationNeededWhenNoRdsView() {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        RdsView rdsView = null;
        when(source.getRdsView(DatabaseType.ATLAS)).thenReturn(rdsView);

        Assertions.assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("isConfigurationNeededArguments")
    void testIsConfigurationNeeded(String name, StackType stackType, boolean atlasInBlueprint, boolean hbasePresent, boolean hdfsPresent,
            String cmVersion, String cdhVersion, boolean rdsViewExists, boolean expectedResult) {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        RdsView rdsView = rdsViewExists ? mock(RdsView.class) : null;

        setupMocksForIsConfigurationNeeded(cmTemplateProcessor, source, rdsView, stackType, atlasInBlueprint, hbasePresent, hdfsPresent, cmVersion, cdhVersion);

        assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, source)).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> isConfigurationNeededArguments() {
        return Stream.of(
                Arguments.of("All conditions met", StackType.DATALAKE, true, false, false, "7.13.2.10001", "7.3.2", true, true),
                Arguments.of("Not DataLake", StackType.WORKLOAD, true, false, false, "7.13.2.10001", "7.3.2", true, false),
                Arguments.of("HBase present", StackType.DATALAKE, true, true, false, "7.13.2.10001", "7.3.2", true, false),
                Arguments.of("HDFS present", StackType.DATALAKE, true, false, true, "7.13.2.10001", "7.3.2", true, false),
                Arguments.of("Atlas not present in blueprint", StackType.DATALAKE, false, false, false, "7.13.2.10001", "7.3.2", true, false),
                Arguments.of("CM version too old", StackType.DATALAKE, true, false, false, "7.13.1", "7.3.2", true, false),
                Arguments.of("CDH version too old", StackType.DATALAKE, true, false, false, "7.13.2.10001", "7.3.1", true, false),
                Arguments.of("RDS view missing", StackType.DATALAKE, true, false, false, "7.13.2.10001", "7.3.2", false, false)
        );
    }

    private void setupMocksForIsConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source, RdsView rdsView,
            StackType stackType, boolean atlasInBlueprint, boolean hbasePresent, boolean hdfsPresent, String cmVersion, String cdhVersion) {
        lenient().when(source.getRdsView(DatabaseType.ATLAS)).thenReturn(rdsView);
        if (rdsView != null) {
            lenient().when(cmTemplateProcessor.isRoleTypePresentInService("ATLAS", List.of("ATLAS_SERVER"))).thenReturn(true);
        }
        lenient().when(source.getStackType()).thenReturn(stackType);
        lenient().when(cmTemplateProcessor.doesCMComponentExistsInBlueprint("ATLAS_SERVER")).thenReturn(atlasInBlueprint);
        lenient().when(cmTemplateProcessor.isServiceTypePresent(HbaseRoles.HBASE)).thenReturn(hbasePresent);
        lenient().when(cmTemplateProcessor.isServiceTypePresent(HdfsRoles.HDFS)).thenReturn(hdfsPresent);

        ProductDetailsView productDetailsView = mock(ProductDetailsView.class);
        ClouderaManagerRepo cmRepo = mock(ClouderaManagerRepo.class);
        lenient().when(source.getProductDetailsView()).thenReturn(productDetailsView);
        lenient().when(productDetailsView.getCm()).thenReturn(cmRepo);
        lenient().when(cmRepo.getVersion()).thenReturn(cmVersion);

        BlueprintView blueprintView = mock(BlueprintView.class);
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        lenient().when(source.getBlueprintView()).thenReturn(blueprintView);
        lenient().when(blueprintView.getProcessor()).thenReturn(blueprintTextProcessor);
        lenient().when(blueprintTextProcessor.getVersion()).thenReturn(Optional.ofNullable(cdhVersion));
    }

    @Test
    void testGetServiceConfigs() {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        RdsView rdsView = mock(RdsView.class);

        when(source.getRdsView(DatabaseType.ATLAS)).thenReturn(rdsView);
        when(rdsView.getHost()).thenReturn("myhost");
        when(rdsView.getDatabaseName()).thenReturn("atlas");
        when(rdsView.getConnectionUserName()).thenReturn("user");
        when(rdsView.getConnectionPassword()).thenReturn("pass");

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertThat(result).hasSize(5);
        assertConfig(result, "atlas_database_host", "myhost");
        assertConfig(result, "atlas_database_name", "atlas");
        assertConfig(result, "atlas_database_user", "user");
        assertConfig(result, "atlas_database_password", "pass");
        assertConfig(result, "atlas_config_safety_valve", "<property><name>atlas_database_type</name><value>PostgreSQL</value></property>");
    }

    private void assertConfig(List<ApiClusterTemplateConfig> configs, String name, String value) {
        assertThat(configs).anyMatch(c -> name.equals(c.getName()) && value.equals(c.getValue()));
    }
}

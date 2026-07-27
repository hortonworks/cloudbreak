package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_13_2_10000;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.ProductDetailsView;

@ExtendWith(MockitoExtension.class)
class HbaseRegionServerExternalTlsConfigProviderTest {

    private static final String CDH_7_3_2_10000 = "7.3.2-1.cdh7.3.2.p10000.80393083";

    private static final String CDH_7_3_2_0 = "7.3.2-1.cdh7.3.2.p0.80393083";

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @Mock
    private ProductDetailsView productDetailsView;

    @InjectMocks
    private HbaseRegionServerExternalTlsConfigProvider underTest;

    @Test
    void getRoleConfigsAddsSp1TlsSettingsToExternalRegionServerRoleConfigGroup() {
        mockProducts(CDH_7_3_2_10000, CLOUDERAMANAGER_VERSION_7_13_2_10000.getVersion());

        List<ApiClusterTemplateConfig> configs = underTest.getRoleConfigs(cmTemplateProcessor, source)
                .get(HbaseRegionServerExternalTlsConfigProvider.REGIONSERVER_EXTERNAL_RCG);

        assertThat(configs)
                .extracting(ApiClusterTemplateConfig::getName, ApiClusterTemplateConfig::getValue)
                .containsExactlyInAnyOrder(
                        tuple("hbase_tls_client_enabled", "true"),
                        tuple("regionserver_tls_plaintext_enabled", "false")
                );
    }

    @Test
    void getRoleConfigsKeepsPlaintextEnabledForPreSp1Runtime() {
        mockProducts(CDH_7_3_2_0, CLOUDERAMANAGER_VERSION_7_13_2_10000.getVersion());

        List<ApiClusterTemplateConfig> configs = underTest.getRoleConfigs(cmTemplateProcessor, source)
                .get(HbaseRegionServerExternalTlsConfigProvider.REGIONSERVER_EXTERNAL_RCG);

        assertThat(configs)
                .extracting(ApiClusterTemplateConfig::getName, ApiClusterTemplateConfig::getValue)
                .containsExactly(tuple("regionserver_tls_plaintext_enabled", "true"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("configurationNeededArguments")
    void isConfigurationNeeded(String displayName, boolean hasExternalRcg, String cdhVersion, String cmVersion, boolean expected) {
        mockExternalRegionServerRoleConfigGroup(hasExternalRcg);
        if (hasExternalRcg) {
            mockCdhProduct(cdhVersion);
            if (requiresCmVersionStub(displayName)) {
                mockCmVersion(cmVersion != null ? cmVersion : "");
            }
        }

        assertEquals(expected, underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sp1FixArguments")
    void isSp1ExternalRegionServerTlsFix(String displayName, String cdhVersion, String cmVersion, boolean expected) {
        mockProducts(cdhVersion, cmVersion);

        assertEquals(expected, HbaseRegionServerExternalTlsConfigProvider.isSp1ExternalRegionServerTlsFix(source));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("runtimeAtLeast732Arguments")
    void isRuntimeAtLeast732(String displayName, String cdhVersion, boolean expected) {
        mockCdhProduct(cdhVersion);

        assertEquals(expected, HbaseRegionServerExternalTlsConfigProvider.isRuntimeAtLeast732(source));
    }

    private static List<Arguments> configurationNeededArguments() {
        return List.of(
                Arguments.of("Supported SP1 versions with external RCG", true, CDH_7_3_2_10000, CLOUDERAMANAGER_VERSION_7_13_2_10000.getVersion(), true),
                Arguments.of("Pre-SP1 runtime with external RCG", true, CDH_7_3_2_0, "7.13.2.0", true),
                Arguments.of("Missing external RCG", false, CDH_7_3_2_10000, CLOUDERAMANAGER_VERSION_7_13_2_10000.getVersion(), false),
                Arguments.of("Runtime below 7.3.2", true, "7.3.1-1.cdh7.3.1.p0.80393083", CLOUDERAMANAGER_VERSION_7_13_2_10000.getVersion(), false),
                Arguments.of("Missing CM version", true, CDH_7_3_2_10000, null, false)
        );
    }

    private static List<Arguments> sp1FixArguments() {
        return List.of(
                Arguments.of("Supported SP1 pair", CDH_7_3_2_10000, CLOUDERAMANAGER_VERSION_7_13_2_10000.getVersion(), true),
                Arguments.of("Runtime below SP1", CDH_7_3_2_0, CLOUDERAMANAGER_VERSION_7_13_2_10000.getVersion(), false),
                Arguments.of("CM below SP1", CDH_7_3_2_10000, "7.13.2.0", false)
        );
    }

    private static List<Arguments> runtimeAtLeast732Arguments() {
        return List.of(
                Arguments.of("Runtime 7.3.2.0", CDH_7_3_2_0, true),
                Arguments.of("Runtime 7.3.2.10000", CDH_7_3_2_10000, true),
                Arguments.of("Runtime 7.3.1", "7.3.1-1.cdh7.3.1.p0.80393083", false)
        );
    }

    private void mockExternalRegionServerRoleConfigGroup(boolean present) {
        if (!present) {
            when(cmTemplateProcessor.getServiceByType(HbaseRoles.HBASE)).thenReturn(java.util.Optional.empty());
            return;
        }
        ApiClusterTemplateRoleConfigGroup externalRegionServer = new ApiClusterTemplateRoleConfigGroup()
                .refName(HbaseRegionServerExternalTlsConfigProvider.REGIONSERVER_EXTERNAL_RCG)
                .roleType(HbaseRoles.REGIONSERVER);
        ApiClusterTemplateService hbaseService = new ApiClusterTemplateService()
                .serviceType(HbaseRoles.HBASE)
                .roleConfigGroups(List.of(externalRegionServer));
        when(cmTemplateProcessor.getServiceByType(HbaseRoles.HBASE)).thenReturn(java.util.Optional.of(hbaseService));
    }

    private void mockCdhProduct(String cdhVersion) {
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct();
        cdhProduct.setName("CDH");
        cdhProduct.setVersion(cdhVersion);
        when(source.getProductDetailsView()).thenReturn(productDetailsView);
        when(productDetailsView.getProducts()).thenReturn(List.of(cdhProduct));
    }

    private void mockCmVersion(String cmVersion) {
        when(source.getProductDetailsView()).thenReturn(productDetailsView);
        when(productDetailsView.getCm()).thenReturn(new com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo().withVersion(cmVersion));
    }

    private void mockProducts(String cdhVersion, String cmVersion) {
        mockCdhProduct(cdhVersion);
        mockCmVersion(cmVersion != null ? cmVersion : "");
    }

    private static boolean requiresCmVersionStub(String displayName) {
        return !"Runtime below 7.3.2".equals(displayName);
    }

    private static org.assertj.core.groups.Tuple tuple(String name, String value) {
        return org.assertj.core.groups.Tuple.tuple(name, value);
    }
}

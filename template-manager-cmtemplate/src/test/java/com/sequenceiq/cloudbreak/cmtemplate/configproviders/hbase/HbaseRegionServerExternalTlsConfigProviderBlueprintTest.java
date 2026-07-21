package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_13_2_10000;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

class HbaseRegionServerExternalTlsConfigProviderBlueprintTest {

    private static final String CDH_7_3_2_10000 = "7.3.2-1.cdh7.3.2.p10000.80393083";

    private static final String CDH_7_3_2_0 = "7.3.2-1.cdh7.3.2.p0.80393083";

    private static final Path ENTERPRISE_BLUEPRINT = Path.of(
            "../core/src/main/resources/defaults/blueprints/7.3.2/cdp-sdx-enterprise.bp");

    private final HbaseRegionServerExternalTlsConfigProvider underTest = new HbaseRegionServerExternalTlsConfigProvider();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void enterprise732BlueprintDoesNotPinPlaintextOnExternalRegionServer() throws IOException {
        assumeTrue(Files.exists(ENTERPRISE_BLUEPRINT), "7.3.2 enterprise blueprint file is not available");

        CmTemplateProcessor processor = loadEnterpriseBlueprintProcessor();

        List<ApiClusterTemplateConfig> externalConfigs = processor.getServiceByType(HbaseRoles.HBASE)
                .flatMap(service -> service.getRoleConfigGroups().stream()
                        .filter(rcg -> HbaseRegionServerExternalTlsConfigProvider.REGIONSERVER_EXTERNAL_RCG.equals(rcg.getRefName()))
                        .findFirst()
                        .map(ApiClusterTemplateRoleConfigGroup::getConfigs))
                .orElse(List.of());

        assertFalse(externalConfigs.stream()
                .anyMatch(config -> "regionserver_tls_plaintext_enabled".equals(config.getName())));
    }

    @Test
    void providerMergesSp1TlsSettingsInto732EnterpriseBlueprint() throws IOException {
        assumeTrue(Files.exists(ENTERPRISE_BLUEPRINT), "7.3.2 enterprise blueprint file is not available");

        CmTemplateProcessor processor = loadEnterpriseBlueprintProcessor();
        TemplatePreparationObject source = templatePreparationObject(CDH_7_3_2_10000, CLOUDERAMANAGER_VERSION_7_13_2_10000.getVersion());

        assertThat(underTest.isConfigurationNeeded(processor, source)).isTrue();
        processor.addRoleConfigs(HbaseRoles.HBASE, underTest.getRoleConfigs(processor, source));

        assertExternalRegionServerConfigValues(processor, Map.of(
                "hbase_tls_client_enabled", "true",
                "regionserver_tls_plaintext_enabled", "false"
        ));
    }

    @Test
    void providerMergesPlaintextTrueInto732EnterpriseBlueprintForPreSp1Runtime() throws IOException {
        assumeTrue(Files.exists(ENTERPRISE_BLUEPRINT), "7.3.2 enterprise blueprint file is not available");

        CmTemplateProcessor processor = loadEnterpriseBlueprintProcessor();
        TemplatePreparationObject source = templatePreparationObject(CDH_7_3_2_0, "7.13.2.0");

        assertThat(underTest.isConfigurationNeeded(processor, source)).isTrue();
        processor.addRoleConfigs(HbaseRoles.HBASE, underTest.getRoleConfigs(processor, source));

        assertExternalRegionServerConfigValues(processor, Map.of(
                "regionserver_tls_plaintext_enabled", "true"
        ));
    }

    private CmTemplateProcessor loadEnterpriseBlueprintProcessor() throws IOException {
        JsonNode root = objectMapper.readTree(Files.readString(ENTERPRISE_BLUEPRINT));
        return new CmTemplateProcessor(root.get("blueprint").toString());
    }

    private TemplatePreparationObject templatePreparationObject(String cdhVersion, String cmVersion) {
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct();
        cdhProduct.setName("CDH");
        cdhProduct.setVersion(cdhVersion);
        return TemplatePreparationObject.Builder.builder()
                .withProductDetails(new ClouderaManagerRepo().withVersion(cmVersion), List.of(cdhProduct))
                .build();
    }

    private void assertExternalRegionServerConfigValues(CmTemplateProcessor processor, Map<String, String> expectedValues) {
        Map<String, String> actualValues = processor.getServiceByType(HbaseRoles.HBASE)
                .flatMap(service -> service.getRoleConfigGroups().stream()
                        .filter(rcg -> HbaseRegionServerExternalTlsConfigProvider.REGIONSERVER_EXTERNAL_RCG.equals(rcg.getRefName()))
                        .findFirst())
                .map(ApiClusterTemplateRoleConfigGroup::getConfigs)
                .orElse(List.of())
                .stream()
                .collect(java.util.stream.Collectors.toMap(ApiClusterTemplateConfig::getName, ApiClusterTemplateConfig::getValue));

        assertThat(actualValues).containsAllEntriesOf(expectedValues);
    }
}

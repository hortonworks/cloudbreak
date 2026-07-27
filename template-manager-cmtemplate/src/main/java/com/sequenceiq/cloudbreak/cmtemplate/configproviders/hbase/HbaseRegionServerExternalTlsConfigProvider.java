package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_13_2_10000;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2_10000;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getCmVersion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;

@Component
public class HbaseRegionServerExternalTlsConfigProvider implements CmTemplateComponentConfigProvider {

    static final String REGIONSERVER_EXTERNAL_RCG = "hbase-REGIONSERVER-EXTERNAL";

    private static final String HBASE_TLS_CLIENT_ENABLED = "hbase_tls_client_enabled";

    private static final String REGIONSERVER_TLS_PLAINTEXT_ENABLED = "regionserver_tls_plaintext_enabled";

    @Override
    public String getServiceType() {
        return HbaseRoles.HBASE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HbaseRoles.REGIONSERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return hasExternalRegionServerRoleConfigGroup(cmTemplateProcessor)
                && isRuntimeAtLeast732(source)
                && StringUtils.hasText(getCmVersion(source));
    }

    @Override
    public Map<String, List<ApiClusterTemplateConfig>> getRoleConfigs(CmTemplateProcessor cmTemplate, TemplatePreparationObject source) {
        return Map.of(REGIONSERVER_EXTERNAL_RCG, getExternalRegionServerTlsConfigs(source));
    }

    static boolean isSp1ExternalRegionServerTlsFix(TemplatePreparationObject source) {
        Optional<String> cdhFullVersion = getCdhFullVersion(source);
        if (cdhFullVersion.isEmpty() || !StringUtils.hasText(getCmVersion(source))) {
            return false;
        }
        return isVersionNewerOrEqualThanLimited(cdhFullVersion.get(), CLOUDERA_STACK_VERSION_7_3_2_10000)
                && isVersionNewerOrEqualThanLimited(normalizeCmVersion(getCmVersion(source)), CLOUDERAMANAGER_VERSION_7_13_2_10000);
    }

    static boolean isRuntimeAtLeast732(TemplatePreparationObject source) {
        return getCdhFullVersion(source)
                .map(cdhFullVersion -> isVersionNewerOrEqualThanLimited(cdhFullVersion, CLOUDERA_STACK_VERSION_7_3_2))
                .orElse(false);
    }

    private static boolean hasExternalRegionServerRoleConfigGroup(CmTemplateProcessor cmTemplateProcessor) {
        return cmTemplateProcessor.getServiceByType(HbaseRoles.HBASE)
                .map(service -> service.getRoleConfigGroups().stream()
                        .map(ApiClusterTemplateRoleConfigGroup::getRefName)
                        .anyMatch(REGIONSERVER_EXTERNAL_RCG::equals))
                .orElse(false);
    }

    private static List<ApiClusterTemplateConfig> getExternalRegionServerTlsConfigs(TemplatePreparationObject source) {
        return getExternalRegionServerTlsConfigValues(source).entrySet().stream()
                .map(entry -> config(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static Map<String, String> getExternalRegionServerTlsConfigValues(TemplatePreparationObject source) {
        Map<String, String> configValues = new LinkedHashMap<>();
        if (isSp1ExternalRegionServerTlsFix(source)) {
            configValues.put(HBASE_TLS_CLIENT_ENABLED, "true");
            configValues.put(REGIONSERVER_TLS_PLAINTEXT_ENABLED, "false");
        } else {
            configValues.put(REGIONSERVER_TLS_PLAINTEXT_ENABLED, "true");
        }
        return configValues;
    }

    private static Optional<String> getCdhFullVersion(TemplatePreparationObject source) {
        if (source.getProductDetailsView() == null || source.getProductDetailsView().getProducts() == null) {
            return Optional.empty();
        }
        return source.getProductDetailsView().getProducts().stream()
                .filter(product -> "CDH".equals(product.getName()))
                .findFirst()
                .map(ClouderaManagerProduct::getVersion)
                .map(CdhVersionProvider::getCdhFullVersionFromVersionString)
                .filter(StringUtils::hasText);
    }

    private static String normalizeCmVersion(String cmVersion) {
        return cmVersion.split("-")[0];
    }
}

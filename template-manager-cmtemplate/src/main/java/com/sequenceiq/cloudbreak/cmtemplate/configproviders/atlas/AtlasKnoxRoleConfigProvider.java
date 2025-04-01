package com.sequenceiq.cloudbreak.cmtemplate.configproviders.atlas;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class AtlasKnoxRoleConfigProvider extends AbstractRoleConfigProvider {

    public static final String MIN_VERSION_FOR_DIFFERENCIAL_BACKUP = "7.2.7";

    public static final String MAX_VERSION_FOR_DIFFERENCIAL_BACKUP = "7.2.16";

    private static final String ATLAS_KNOX_CONFIG = "atlas_authentication_method_trustedproxy";

    private static final String ATLAS_DIFFERENTIAL_AUDIT_CONFIG = "atlas.entity.audit.differential";

    private static final Logger LOG = LoggerFactory.getLogger(AtlasKnoxRoleConfigProvider.class);

    private final ExposedServiceCollector exposedServiceCollector;

    private final EntitlementService entitlementService;

    private final ObjectMapper objectMapper;

    public AtlasKnoxRoleConfigProvider(EntitlementService entitlementService,
            ExposedServiceCollector exposedServiceCollector,
            ObjectMapper objectMapper) {
        this.entitlementService = entitlementService;
        this.exposedServiceCollector = exposedServiceCollector;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        if (isWireEncryptionEnabled(source)) {
            LOG.info(String.format("SDX Optimization Enabled for %s.", roleType));
            configs.add(config(ATLAS_DIFFERENTIAL_AUDIT_CONFIG, "true"));
        }
        if (isAtlasKnoxConfigurationNeeded(source)) {
            configs.add(config(ATLAS_KNOX_CONFIG, "true"));
        }
        return configs;
    }

    @Override
    public String getServiceType() {
        return "ATLAS";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of("ATLAS_SERVER");
    }

    private boolean isAtlasKnoxConfigurationNeeded(TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().contains(exposedServiceCollector.getAtlasService().getKnoxService());
    }

    private boolean isWireEncryptionEnabled(TemplatePreparationObject source) {
        return !CloudPlatform.YARN.equals(source.getCloudPlatform())
                && StackType.DATALAKE.equals(source.getStackType())
                && entitlementService.isWireEncryptionEnabled(ThreadBasedUserCrnProvider.getAccountId())
                && isDatalakeVersionSupported(source.getBlueprintView().getBlueprintText());
    }

    @VisibleForTesting
    public boolean isDatalakeVersionSupported(String blueprintText) {
        if (StringUtils.isEmpty(blueprintText)) {
            LOG.error("Empty blueprint arrived for AtlasKnox Role config.");
            throw new IllegalArgumentException("Blueprint is empty.");
        }
        String versionString;
        try {
            versionString = objectMapper.readValue(blueprintText, CdhVersion.class).getCdhVersion();
        } catch (JsonProcessingException e) {
            LOG.error("Invalid blueprint arrived for AtlasKnox Role config. Error message: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        LOG.info("incoming version: {}", versionString);
        return isShapeVersionSupportedByRuntime(versionString);
    }

    private boolean isShapeVersionSupportedByRuntime(String runtime) {
        Comparator<Versioned> versionComparator = new VersionComparator();
        int maxVersionCheck = versionComparator.compare(() -> runtime, () -> MAX_VERSION_FOR_DIFFERENCIAL_BACKUP);
        return versionComparator.compare(() -> runtime, () -> MIN_VERSION_FOR_DIFFERENCIAL_BACKUP) > -1
                && maxVersionCheck < 1;
    }

    @VisibleForTesting
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CdhVersion {

        @JsonProperty("cdhVersion")
        private String cdhVersion;

        public CdhVersion() {
        }

        public CdhVersion(String cdhVersion) {
            this.cdhVersion = cdhVersion;
        }

        public String getCdhVersion() {
            return cdhVersion;
        }

        public void setCdhVersion(String cdhVersion) {
            this.cdhVersion = cdhVersion;
        }

    }
}

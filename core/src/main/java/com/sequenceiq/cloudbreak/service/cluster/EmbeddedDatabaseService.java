package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class EmbeddedDatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDatabaseService.class);

    private static final String SSL_ENFORCEMENT_MIN_RUNTIME_VERSION = "7.2.2";

    @Inject
    private CloudParameterCache cloudParameterCache;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private BlueprintService blueprintService;

    public boolean isEmbeddedDatabaseOnAttachedDiskEnabled(StackDtoDelegate stack, ClusterView cluster) {
        return isEmbeddedDatabaseOnAttachedDiskEnabledInternal(stack.getExternalDatabaseCreationType(), stack.getCloudPlatform(), cluster);
    }

    boolean isEmbeddedDatabaseOnAttachedDiskEnabledByStackView(StackView stack, ClusterView cluster, Database database) {
        return isEmbeddedDatabaseOnAttachedDiskEnabledInternal(database != null ? database.getExternalDatabaseAvailabilityType() : null,
                stack.getCloudPlatform(), cluster);
    }

    private boolean isEmbeddedDatabaseOnAttachedDiskEnabledInternal(DatabaseAvailabilityType externalDatabaseCreationType, String cloudPlatform,
            ClusterView cluster) {
        DatabaseAvailabilityType externalDatabase = ObjectUtils.defaultIfNull(externalDatabaseCreationType, DatabaseAvailabilityType.NONE);
        String databaseCrn = cluster == null ? "" : cluster.getDatabaseServerCrn();
        return DatabaseAvailabilityType.NONE == externalDatabase
                && StringUtils.isEmpty(databaseCrn)
                && cloudParameterCache.isVolumeAttachmentSupported(cloudPlatform);
    }

    public boolean isAttachedDiskForEmbeddedDatabaseCreated(StackDto stack) {
        Optional<InstanceGroupView> gatewayGroup = stack.getGatewayGroup();
        return stack.getCluster().getEmbeddedDatabaseOnAttachedDisk()
                && calculateVolumeCountOnGatewayGroup(gatewayGroup.map(InstanceGroupView::getTemplate)) > 0;
    }

    public boolean isAttachedDiskForEmbeddedDatabaseCreated(ClusterView cluster, Optional<InstanceGroupView> gatewayGroup) {
        return cluster.getEmbeddedDatabaseOnAttachedDisk() && calculateVolumeCountOnGatewayGroup(gatewayGroup.map(InstanceGroupView::getTemplate)) > 0;
    }

    public boolean isSslEnforcementForEmbeddedDatabaseEnabled(StackView stackView, ClusterView clusterView, Database database) {
        StackType stackType = stackView.getType();
        boolean sslEnforcementEnabled = stackType == StackType.DATALAKE || stackType == StackType.WORKLOAD;
        String runtime = getRuntime(clusterView);
        boolean response = sslEnforcementEnabled && isEmbeddedDatabaseOnAttachedDiskEnabledByStackView(stackView, clusterView, database) &&
                isSslEnforcementSupportedForRuntime(runtime);
        LOGGER.info("Embedded DB SSL enforcement is {} for runtime version {}", response ? "enabled" : "disabled", runtime);
        return response;
    }

    private String getRuntime(ClusterView clusterView) {
        String runtime = null;
        Optional<String> blueprintTextOpt = blueprintService.getByClusterId(clusterView.getId()).map(Blueprint::getBlueprintJsonText);
        if (blueprintTextOpt.isPresent()) {
            CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintTextOpt.get());
            runtime = cmTemplateProcessor.getStackVersion();
            LOGGER.info("Blueprint text is available for stack, found runtime version '{}'", runtime);
        } else {
            LOGGER.warn("Blueprint text is unavailable for stack, thus runtime version cannot be determined.");
        }
        return runtime;
    }

    private boolean isSslEnforcementSupportedForRuntime(String runtime) {
        if (StringUtils.isBlank(runtime)) {
            // While this may happen for custom data lakes, it is not possible for DH clusters
            LOGGER.info("Runtime version is NOT specified, embedded DB SSL enforcement is NOT permitted");
            return false;
        }
        boolean permitted = CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited(() -> runtime, () -> SSL_ENFORCEMENT_MIN_RUNTIME_VERSION);
        LOGGER.info("Embedded DB SSL enforcement {} permitted for runtime version {}", permitted ? "is" : "is NOT", runtime);
        return permitted;
    }

    private int calculateVolumeCountOnGatewayGroup(Optional<Template> gatewayGroupTemplate) {
        Template template = gatewayGroupTemplate.orElse(null);
        return template == null ? 0 : template.getVolumeTemplates().stream()
                .filter(volumeTemplate -> volumeTemplate.getUsageType() == VolumeUsageType.DATABASE)
                .mapToInt(VolumeTemplate::getVolumeCount).sum();
    }

}

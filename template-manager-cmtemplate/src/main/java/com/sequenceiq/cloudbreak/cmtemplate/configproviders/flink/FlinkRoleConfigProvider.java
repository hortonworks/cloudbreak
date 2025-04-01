package com.sequenceiq.cloudbreak.cmtemplate.configproviders.flink;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class FlinkRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String HISTORY_SERVER_ARCHIVE_FS_DIR = "historyserver_archive_fs_dir";

    private static final String JOBMANAGER_ARCHIVE_FS_DIR = "jobmanager_archive_fs_dir";

    private static final String STATE_CHECKPOINTS_DIR = "state_checkpoints_dir";

    private static final String STATE_SAVEPOINTS_DIR = "state_savepoints_dir";

    private static final String HIGH_AVAILABILITY_STORAGE_DIR = "high_availability_storage_dir";

    private static final String ATLAS_COLLECTION_ENABLED = "atlas_collection_enabled";

    @Inject
    private FlinkConfigProviderUtils utils;

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        if (FlinkConstants.FLINK_HISTORY_SERVER.equals(roleType)) {
            return ConfigUtils.getStorageLocationForServiceProperty(source, HISTORY_SERVER_ARCHIVE_FS_DIR)
                    .map(location -> List.of(config(HISTORY_SERVER_ARCHIVE_FS_DIR, location.getValue())))
                    .orElseGet(List::of);
        } else {
            return List.of();
        }
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> serviceConfigs = new ArrayList<>();
        ConfigUtils.getStorageLocationForServiceProperty(source, JOBMANAGER_ARCHIVE_FS_DIR)
                .ifPresent(location -> serviceConfigs.add(config(JOBMANAGER_ARCHIVE_FS_DIR, location.getValue())));
        ConfigUtils.getStorageLocationForServiceProperty(source, STATE_CHECKPOINTS_DIR)
                .ifPresent(location -> serviceConfigs.add(config(STATE_CHECKPOINTS_DIR, location.getValue())));
        ConfigUtils.getStorageLocationForServiceProperty(source, STATE_SAVEPOINTS_DIR)
                .ifPresent(location -> serviceConfigs.add(config(STATE_SAVEPOINTS_DIR, location.getValue())));
        ConfigUtils.getStorageLocationForServiceProperty(source, HIGH_AVAILABILITY_STORAGE_DIR)
                .ifPresent(location -> serviceConfigs.add(config(HIGH_AVAILABILITY_STORAGE_DIR, location.getValue())));
        serviceConfigs.add(config(ATLAS_COLLECTION_ENABLED, "true"));

        String cdhVersion = getCdhVersion(source);
        Optional<ClouderaManagerProduct> flinkProduct = utils.getFlinkProduct(source.getProductDetailsView().getProducts());
        utils.addReleaseNameIfNeeded(cdhVersion, serviceConfigs, flinkProduct);

        return serviceConfigs;
    }

    @Override
    public String getServiceType() {
        return FlinkConstants.FLINK;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(FlinkConstants.GATEWAY, FlinkConstants.FLINK_HISTORY_SERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}

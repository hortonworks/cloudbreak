package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class HiveMetastoreCloudStorageRoleConfigProvider extends AbstractRoleConfigConfigProvider {

    private static final String HMS_CONFIG_SAFETY_VALVE = "hive_metastore_config_safety_valve";

    private static final String HMS_METASTORE_DIR = "hive.metastore.warehouse.dir";

    private static final String HMS_METASTORE_EXTERNAL_DIR = "hive.metastore.warehouse.external.dir";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        switch (roleType) {
            case HiveRoles.HIVEMETASTORE:
                String cloudStorageProperty = getCloudStorageProperty(source);
                if (!cloudStorageProperty.isEmpty()) {
                    return List.of(config(HMS_CONFIG_SAFETY_VALVE, cloudStorageProperty));
                }
                return List.of();
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return HiveRoles.HIVE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HiveRoles.HIVEMETASTORE);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    protected String getCloudStorageProperty(TemplatePreparationObject source) {
        StringBuilder hmsCloudStorage = new StringBuilder();

        ConfigUtils.getStorageLocationForServiceProperty(source, HMS_METASTORE_DIR).ifPresent(
                storageLocation -> hmsCloudStorage.append(
                        ConfigUtils.getSafetyValveProperty(HMS_METASTORE_DIR, storageLocation.getValue()))
        );
        ConfigUtils.getStorageLocationForServiceProperty(source, HMS_METASTORE_EXTERNAL_DIR).ifPresent(
                storageLocation -> hmsCloudStorage.append(
                        ConfigUtils.getSafetyValveProperty(HMS_METASTORE_EXTERNAL_DIR, storageLocation.getValue()))
        );
        return hmsCloudStorage.toString();
    }
}

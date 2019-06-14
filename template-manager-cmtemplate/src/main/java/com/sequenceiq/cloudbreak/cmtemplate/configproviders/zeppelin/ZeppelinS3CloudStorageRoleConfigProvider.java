package com.sequenceiq.cloudbreak.cmtemplate.configproviders.zeppelin;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.zeppelin.ZeppelinRoles.ZEPPELIN_SERVER;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.common.type.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class ZeppelinS3CloudStorageRoleConfigProvider extends AbstractRoleConfigConfigProvider {

    private static final String ZEPPELIN_NOTEBOOK_STORAGE = "zeppelin.notebook.storage";

    private static final String ZEPPELIN_SITE_SAFETY_VALVE = "zeppelin-conf/zeppelin-site.xml_role_safety_valve";

    private static final String ZEPPELIN_NOTEBOOK_S3_BUCKET = "zeppelin.notebook.s3.bucket";

    private static final String ZEPPELIN_NOTEBOOK_S3_USER = "zeppelin.notebook.s3.user";

    private static final String ZEPPELIN_S3_NOTEBOOK_REPO = "org.apache.zeppelin.notebook.repo.S3NotebookRepo";

    private static final String S3A_PREFIX = FileSystemType.S3.getProtocol() + "://";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        switch (roleType) {

            case ZEPPELIN_SERVER:
                String cloudStorageProperty = getCloudStorageProperty(source);
                if (!cloudStorageProperty.isEmpty()) {
                    return List.of(
                            config(ZEPPELIN_SITE_SAFETY_VALVE, cloudStorageProperty),
                            config(ZEPPELIN_NOTEBOOK_STORAGE, ZEPPELIN_S3_NOTEBOOK_REPO)
                    );
                }
                return List.of();
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return ZeppelinRoles.ZEPPELIN;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(ZEPPELIN_SERVER);
    }

    protected String getCloudStorageProperty(TemplatePreparationObject source) {
        StringBuilder hmsCloudStorage = new StringBuilder();

        ConfigUtils.getStorageLocationForServiceProperty(source, ZEPPELIN_NOTEBOOK_S3_BUCKET).ifPresent(
                storageLocation -> hmsCloudStorage.append(
                        ConfigUtils.getSafetyValveProperty(ZEPPELIN_NOTEBOOK_S3_BUCKET,
                                storageLocation.getValue().replace(S3A_PREFIX, "")))
        );

        String zeppelinUser = ConfigUtils.getStorageLocationForServiceProperty(source, ZEPPELIN_NOTEBOOK_S3_USER)
                .map(StorageLocationView::getValue)
                .orElse(source.getGeneralClusterConfigs().getClusterName());
        hmsCloudStorage.append(ConfigUtils.getSafetyValveProperty(ZEPPELIN_NOTEBOOK_S3_USER, zeppelinUser));

        return hmsCloudStorage.toString();
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && source.getFileSystemConfigurationView().get().getType().equals(FileSystemType.S3.name())
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}
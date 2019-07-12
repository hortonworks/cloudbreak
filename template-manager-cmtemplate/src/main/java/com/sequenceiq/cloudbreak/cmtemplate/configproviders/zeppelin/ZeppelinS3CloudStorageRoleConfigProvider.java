package com.sequenceiq.cloudbreak.cmtemplate.configproviders.zeppelin;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.zeppelin.ZeppelinRoles.ZEPPELIN_SERVER;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.common.api.filesystem.FileSystemType;

@Component
public class ZeppelinS3CloudStorageRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String ZEPPELIN_NOTEBOOK_STORAGE = "zeppelin.notebook.storage";

    private static final String ZEPPELIN_SITE_SAFETY_VALVE = "zeppelin-conf/zeppelin-site.xml_role_safety_valve";

    private static final String ZEPPELIN_NOTEBOOK_S3_BUCKET = "zeppelin.notebook.s3.bucket";

    private static final String ZEPPELIN_NOTEBOOK_S3_USER = "zeppelin.notebook.s3.user";

    private static final String ZEPPELIN_S3_NOTEBOOK_REPO = "org.apache.zeppelin.notebook.repo.S3NotebookRepo";

    private static final Pattern S3A_BUCKET_PATTERN = Pattern.compile(FileSystemType.S3.getProtocol() + "://([\\w.-]+)(?:/(.*))?");

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
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

        ConfigUtils.getStorageLocationForServiceProperty(source, ZEPPELIN_NOTEBOOK_S3_BUCKET)
                .ifPresent(storageLocation -> {
                    hmsCloudStorage.append(
                            ConfigUtils.getSafetyValveProperty(ZEPPELIN_NOTEBOOK_S3_BUCKET,
                                    S3A_BUCKET_PATTERN.matcher(storageLocation.getValue()).replaceAll("$1")));

                    hmsCloudStorage.append(ConfigUtils.getSafetyValveProperty(ZEPPELIN_NOTEBOOK_S3_USER,
                            S3A_BUCKET_PATTERN.matcher(storageLocation.getValue()).replaceAll("$2")));
                });

        return hmsCloudStorage.toString();
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && source.getFileSystemConfigurationView().get().getType().equals(FileSystemType.S3.name())
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}

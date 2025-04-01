package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiRoles.NIFI_NODE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class NifiCloudStorageRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String NIFI_LOG_DIR_COPY_TO_OBJECT_STORAGE = "nifi.log.dir.copy.to.cloud.object.storage";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor,
            TemplatePreparationObject templatePreparationObject) {
        switch (roleType) {

            case NIFI_NODE:
                return ConfigUtils.getStorageLocationForServiceProperty(templatePreparationObject, NIFI_LOG_DIR_COPY_TO_OBJECT_STORAGE)
                        .map(location -> List.of(config(NIFI_LOG_DIR_COPY_TO_OBJECT_STORAGE, location.getValue())))
                        .orElseGet(List::of);
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return NifiRoles.NIFI;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(NIFI_NODE);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        boolean is730OrNewer = isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_3_0);
        return source.getFileSystemConfigurationView().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes())
                && is730OrNewer;
    }
}
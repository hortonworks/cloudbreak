package com.sequenceiq.cloudbreak.cmtemplate.configproviders.zeppelin;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.zeppelin.ZeppelinRoles.ZEPPELIN_SERVER;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class ZeppelinCloudStorageRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String ZEPPELIN_NOTEBOOK_DIR = "zeppelin.notebook.dir";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor,
            TemplatePreparationObject templatePreparationObject) {
        switch (roleType) {

            case ZEPPELIN_SERVER:
                return ConfigUtils.getStorageLocationForServiceProperty(templatePreparationObject, ZEPPELIN_NOTEBOOK_DIR)
                        .map(location -> List.of(config(ZEPPELIN_NOTEBOOK_DIR, location.getValue())))
                        .orElseGet(List::of);
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

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}
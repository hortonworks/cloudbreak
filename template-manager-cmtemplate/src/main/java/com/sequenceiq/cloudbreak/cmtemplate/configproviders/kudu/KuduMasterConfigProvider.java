package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kudu;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_11;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class KuduMasterConfigProvider extends AbstractRoleConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        ArrayList<ApiClusterTemplateConfig> configs = Lists.newArrayList();

        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
            "" : source.getBlueprintView().getProcessor().getStackVersion();
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_2_11)) {
            configs.add(config(KuduConfigs.RANGER_KUDU_PLUGIN_SERVICE_NAME, KuduConfigs.GENERATED_RANGER_SERVICE_NAME));
        }

        return configs;
    }

    @Override
    public String getServiceType() {
        return KuduRoles.KUDU_SERVICE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(KuduRoles.KUDU_MASTER);
    }
}

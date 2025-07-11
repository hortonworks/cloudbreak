package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class HiveHybridRoleConfigProvider implements CmTemplateComponentConfigProvider {

    protected static final String HIVE_HOOK_PROTO_BASE_DIRECTORY_KEY = "hive_hook_proto_base_directory";

    protected static final String HIVE_HOOK_PROTO_BASE_DIRECTORY_VALUE = "/warehouse/tablespace/managed/hive/sys.db/query_data/";

    @Inject
    private HdfsConfigHelper hdfsConfigHelper;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        hdfsConfigHelper.getAttachedDatalakeHdfsUrlForHybridDatahub(templateProcessor, source)
                .ifPresent(datalakeHdfs -> configs.add(config(HIVE_HOOK_PROTO_BASE_DIRECTORY_KEY, datalakeHdfs + HIVE_HOOK_PROTO_BASE_DIRECTORY_VALUE)));
        return configs;
    }

    @Override
    public String getServiceType() {
        return HiveRoles.HIVE_ON_TEZ;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HiveRoles.HIVESERVER2);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes())
                && cmTemplateProcessor.isHybridDatahub(source);
    }
}

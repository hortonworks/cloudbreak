package com.sequenceiq.cloudbreak.cmtemplate.configproviders.tez;

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
public class TezHybridConfigProvider implements CmTemplateComponentConfigProvider {

    protected static final String TEZ_HISTORY_LOGGING_PROTO_BASE_DIR_KEY = "tez.history.logging.proto-base-dir";

    protected static final String TEZ_HISTORY_LOGGING_PROTO_BASE_DIR_VALUE = "/warehouse/tablespace/managed/hive/sys.db";

    @Inject
    private HdfsConfigHelper hdfsConfigHelper;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        hdfsConfigHelper.getAttachedDatalakeHdfsUrlForHybridDatahub(templateProcessor, source)
                .ifPresent(datalakeHdfs -> configs.add(config(TEZ_HISTORY_LOGGING_PROTO_BASE_DIR_KEY, datalakeHdfs + TEZ_HISTORY_LOGGING_PROTO_BASE_DIR_VALUE)));
        return configs;
    }

    @Override
    public String getServiceType() {
        return TezRoles.TEZ;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(TezRoles.GATEWAY);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes())
                && cmTemplateProcessor.isHybridDatahub(source);
    }
}

package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.s3.S3ConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class HdfsConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String CORE_SITE_SAFETY_VALVE = "core_site_safety_valve";

    @Inject
    private S3ConfigProvider s3ConfigProvider;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        StringBuilder hdfsCoreSiteSafetyValveValue = new StringBuilder();

        s3ConfigProvider.getServiceConfigs(templatePreparationObject, hdfsCoreSiteSafetyValveValue);

        return hdfsCoreSiteSafetyValveValue.toString().isEmpty() ? List.of()
                : List.of(config(CORE_SITE_SAFETY_VALVE, hdfsCoreSiteSafetyValveValue.toString()));
    }

    @Override
    public String getServiceType() {
        return HdfsRoles.HDFS;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HdfsRoles.NAMENODE, HdfsRoles.DATANODE, HdfsRoles.SECONDARYNAMENODE, HdfsRoles.JOURNALNODE);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

}

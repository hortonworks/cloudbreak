package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class HbaseCloudStorageServiceConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String HBASE_ROOT_DIR = "hbase.rootdir";

    private static final String HBASE_ROOT_DIR_TEMPLATE_PARAM = "hdfs_rootdir";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> hbaseConfigs = new ArrayList<>(1);
        ConfigUtils.getStorageLocationForServiceProperty(source, HBASE_ROOT_DIR)
                .ifPresent(location -> hbaseConfigs.add(config(HBASE_ROOT_DIR_TEMPLATE_PARAM, location.getValue())));
        return hbaseConfigs;
    }

    @Override
    public String getServiceType() {
        return HbaseRoles.HBASE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HbaseRoles.MASTER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}
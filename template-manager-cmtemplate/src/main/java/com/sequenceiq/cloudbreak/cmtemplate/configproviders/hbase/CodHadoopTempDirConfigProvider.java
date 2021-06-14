package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildSingleVolumePath;

/**
 * Set hadoop.tmp.dir to attached volume if present.
 * This is for COD clusters only to avoid unwanted side effects for other services.
 */
@Component
public class CodHadoopTempDirConfigProvider implements CmTemplateComponentConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configList = new ArrayList<>();
        String isCodCluster = source.getDefaultTags().get("is_cod_cluster");
        if (Boolean.parseBoolean(isCodCluster) && source.getCloudPlatform() != CloudPlatform.YARN) {
            configList.add(config("hadoop.tmp.dir", buildSingleVolumePath(1, "tmp")));
        }
        return configList;
    }

    @Override
    public String getServiceType() {
        return HbaseRoles.HBASE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HbaseRoles.MASTER, HbaseRoles.REGIONSERVER);
    }
}

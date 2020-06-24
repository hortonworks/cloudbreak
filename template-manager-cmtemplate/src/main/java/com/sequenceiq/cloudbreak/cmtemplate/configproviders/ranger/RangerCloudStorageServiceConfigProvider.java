package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_1;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class RangerCloudStorageServiceConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String RANGER_HDFS_AUDIT_URL = "ranger_plugin_hdfs_audit_url";

    private static final String RANGER_DATA_URL = "cloud_data_location_url";

    private static final String RANGER_LOG_URL = "cloud_log_location_url";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> rangerConfigs = new ArrayList<>();

        ConfigUtils.getStorageLocationForServiceProperty(templatePreparationObject, RANGER_HDFS_AUDIT_URL)
                .map(location -> rangerConfigs.add(config(RANGER_HDFS_AUDIT_URL, location.getValue())))
                .orElseGet(() -> rangerConfigs.add(config(RANGER_HDFS_AUDIT_URL, getDefaultRangerAuditUrl(templateProcessor, templatePreparationObject))));

        String cmVersion = templatePreparationObject.getProductDetailsView().getCm().getVersion();
        //TODO: CB-7709 fix this properly
        if (CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_1)
                && CloudPlatform.AZURE == templatePreparationObject.getCloudPlatform()
                && templatePreparationObject.getGeneralClusterConfigs().isEnableRangerRaz()) {
            //CB-7356 hack, we need to clean this up, see CB-7709.
            String rangerAuditPath =  rangerConfigs.get(0).getValue();
            String baseDataPath = rangerAuditPath.split("ranger/audit")[0];

            rangerConfigs.add(config(RANGER_DATA_URL, baseDataPath));
        }
        return rangerConfigs;
    }

    @Override
    public String getServiceType() {
        return RangerRoles.RANGER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(RangerRoles.RANGER_ADMIN);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    private String getDefaultRangerAuditUrl(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        if (HdfsRoleConfigProvider.isNamenodeHA(templatePreparationObject)) {
            String nameService = templateProcessor.getRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE, "dfs_federation_namenode_nameservice")
                    .map(ApiClusterTemplateConfig::getValue)
                    .orElse(HdfsRoleConfigProvider.DEFAULT_NAME_SERVICE);
            return "hdfs://" + nameService;
        }

        Set<String> namenodeHosts = HdfsRoleConfigProvider.nameNodeFQDNs(templatePreparationObject);
        if (namenodeHosts.size() == 1) {
            return "hdfs://" + namenodeHosts.iterator().next();
        }

        return "";
    }
}
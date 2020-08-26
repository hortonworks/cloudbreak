package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.common.model.CloudStorageCdpService.HIVE_METASTORE_EXTERNAL_WAREHOUSE;
import static com.sequenceiq.common.model.CloudStorageCdpService.HIVE_METASTORE_WAREHOUSE;
import static com.sequenceiq.common.model.CloudStorageCdpService.HIVE_REPLICA_WAREHOUSE;
import static com.sequenceiq.common.model.CloudStorageCdpService.RANGER_AUDIT;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final String HIVE_METASTORE_DIR =  "hive.metastore.warehouse.dir";

    private static final String HIVE_REPLICA_DIR = "hive.repl.replica.functions.root.dir";

    private static final String HIVE_METASTORE_EXTERNAL_DIR = "hive.metastore.warehouse.external.dir";

    private static final String CLOUD_STORAGE_PATHS = "cloud_storage_paths";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        String cmVersion = templatePreparationObject.getProductDetailsView().getCm().getVersion();
        CloudPlatform cloudPlatform = templatePreparationObject.getCloudPlatform();
        if (CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_2)
                && (cloudPlatform.equals(CloudPlatform.AWS) || cloudPlatform.equals(CloudPlatform.AZURE))) {
            List<String> cloudPaths = new ArrayList<>();
            ConfigUtils.getStorageLocationForServiceProperty(templatePreparationObject, HIVE_METASTORE_DIR)
                    .ifPresent(location -> cloudPaths.add(HIVE_METASTORE_WAREHOUSE.name() + "=" + location.getValue()));

            ConfigUtils.getStorageLocationForServiceProperty(templatePreparationObject, HIVE_REPLICA_DIR)
                    .ifPresent(location -> cloudPaths.add(HIVE_REPLICA_WAREHOUSE.name() + "=" + location.getValue()));

            ConfigUtils.getStorageLocationForServiceProperty(templatePreparationObject, HIVE_METASTORE_EXTERNAL_DIR)
                    .ifPresent(location -> cloudPaths.add(HIVE_METASTORE_EXTERNAL_WAREHOUSE.name() + "=" + location.getValue()));

            ConfigUtils.getStorageLocationForServiceProperty(templatePreparationObject, RANGER_HDFS_AUDIT_URL)
                    .map(location -> cloudPaths.add(RANGER_AUDIT.name() + "=" + location.getValue()))
                    .orElseGet(() -> cloudPaths.add(RANGER_AUDIT.name() + "=" + getDefaultRangerAuditUrl(templateProcessor, templatePreparationObject)));


            String cloudPath = cloudPaths.stream().collect(Collectors.joining(","));
            return List.of(getRangerAuditConfig(templateProcessor, templatePreparationObject), config(CLOUD_STORAGE_PATHS, cloudPath));
        } else {
            return List.of(getRangerAuditConfig(templateProcessor, templatePreparationObject));
        }
    }

    private ApiClusterTemplateConfig getRangerAuditConfig(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        return ConfigUtils.getStorageLocationForServiceProperty(templatePreparationObject, RANGER_HDFS_AUDIT_URL)
                .map(location -> config(RANGER_HDFS_AUDIT_URL, location.getValue()))
                .orElseGet(() -> config(RANGER_HDFS_AUDIT_URL, getDefaultRangerAuditUrl(templateProcessor, templatePreparationObject)));
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
package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_7;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

@Component
public class HbaseCloudStorageServiceConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String HBASE_ROOT_DIR = "hbase.rootdir";

    private static final String HBASE_ROOT_DIR_TEMPLATE_PARAM = "hdfs_rootdir";

    @Inject
    private EntitlementService entitlementService;

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
        boolean datalakeCluster = source.getSharedServiceConfigs()
                .map(SharedServiceConfigsView::isDatalakeCluster)
                .orElse(false);
        String cdhVersion = getCdhVersion(source);
        boolean is727OrNewer = isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_2_7);
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        boolean sdxHbaseCloudStorageEnabled = entitlementService.sdxHbaseCloudStorageEnabled(accountId);
        boolean razEnabled =  source.getGeneralClusterConfigs().isEnableRangerRaz();
        boolean awsWithRazDisabled = CloudPlatform.AWS.equals(source.getCloudPlatform()) && !razEnabled;
        return source.getFileSystemConfigurationView().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes())
                && (!datalakeCluster || (is727OrNewer && (awsWithRazDisabled || sdxHbaseCloudStorageEnabled)));
    }

    private String getCdhVersion(TemplatePreparationObject source) {
        if (source.getBlueprintView() == null) {
            return "";
        }

        if (source.getBlueprintView().getProcessor() == null) {
            return "";
        }

        return source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
    }
}
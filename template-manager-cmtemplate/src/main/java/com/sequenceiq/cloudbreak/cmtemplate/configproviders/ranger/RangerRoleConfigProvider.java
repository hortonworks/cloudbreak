package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;


import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.springframework.stereotype.Component;

@Component
public class RangerRoleConfigProvider extends AbstractRdsRoleConfigProvider {
    @Inject
    private VirtualGroupService virtualGroupService;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        String cdhVersion = getCdhVersion(source);
        List<ApiClusterTemplateConfig> configList = new ArrayList<>();

        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_2_0)) {
            addDbConfigs(getRdsView(source), configList);
        }

        return configList;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case RangerRoles.RANGER_ADMIN:
                String cdhVersion = getCdhVersion(source);
                List<ApiClusterTemplateConfig> configList = new ArrayList<>();

                // In CM 7.2.0 and above, the ranger database parameters have moved to the service
                // config (see above getServiceConfigs).
                if (!isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_2_0)) {
                    addDbConfigs(getRdsView(source), configList);
                }

                if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_0_1)) {
                    VirtualGroupRequest virtualGroupRequest = source.getVirtualGroupRequest();
                    String adminGroup = virtualGroupService.getVirtualGroup(virtualGroupRequest, UmsRight.RANGER_ADMIN.getRight());
                    configList.add(config("ranger.default.policy.groups", adminGroup));
                }
                return configList;
            default:
                return List.of();
        }
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
    protected DatabaseType dbType() {
        return DatabaseType.RANGER;
    }

    private String getRangerDbType(RdsView rdsView) {
        switch (rdsView.getDatabaseVendor()) {
            case POSTGRES:
                return "PostgreSQL";
            default:
                throw new CloudbreakServiceException("Unsupported Ranger database type: " + rdsView.getDatabaseVendor().displayName());
        }
    }

    private String getCdhVersion(TemplatePreparationObject source) {
        return source.getBlueprintView().getProcessor().getStackVersion() == null ?
            "" : source.getBlueprintView().getProcessor().getStackVersion();
    }

    private void addDbConfigs(RdsView rangerRdsView, List<ApiClusterTemplateConfig> configList) {
        configList.add(config("ranger_database_host", rangerRdsView.getHost()));
        configList.add(config("ranger_database_name", rangerRdsView.getDatabaseName()));
        configList.add(config("ranger_database_type", getRangerDbType(rangerRdsView)));
        configList.add(config("ranger_database_user", rangerRdsView.getConnectionUserName()));
        configList.add(config("ranger_database_password", rangerRdsView.getConnectionPassword()));
    }
}

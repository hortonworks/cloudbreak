package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class RangerRoleConfigProvider extends AbstractRdsRoleConfigProvider {
    @Inject
    private VirtualGroupService virtualGroupService;

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case RangerRoles.RANGER_ADMIN:
                List<ApiClusterTemplateConfig> configList = new ArrayList<>();
                RdsView rangerRdsView = getRdsView(source);
                configList.add(config("ranger_database_host", rangerRdsView.getHost()));
                configList.add(config("ranger_database_name", rangerRdsView.getDatabaseName()));
                configList.add(config("ranger_database_type", getRangerDbType(rangerRdsView)));
                configList.add(config("ranger_database_user", rangerRdsView.getConnectionUserName()));
                configList.add(config("ranger_database_password", rangerRdsView.getConnectionPassword()));

                String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                        "" : source.getBlueprintView().getProcessor().getStackVersion();
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
}

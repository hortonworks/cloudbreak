package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_8;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getCmVersion;
import static java.util.Collections.emptyList;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class RangerAdminAsSysAdminConfigProvider extends AbstractRoleConfigProvider {

    private static final String RANGER_USERSYNC_GROUP_BASED_ROLE_ASSIGNMENT_RULES = "ranger.usersync.group.based.role.assignment.rules";

    @Override
    public String getServiceType() {
        return RangerRoles.RANGER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(RangerRoles.RANGER_USERSYNC);
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        String cdhVersion = getCmVersion(source);
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_2_8)) {
            return List.of(config(RANGER_USERSYNC_GROUP_BASED_ROLE_ASSIGNMENT_RULES, "ROLE_SYS_ADMIN:u:ranger"));
        }
        return emptyList();
    }
}

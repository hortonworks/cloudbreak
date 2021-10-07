package com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_11;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class OozieRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    private static final String OOZIE_DATABASE_HOST = "oozie_database_host";

    private static final String OOZIE_DATABASE_NAME = "oozie_database_name";

    private static final String OOZIE_DATABASE_TYPE = "oozie_database_type";

    private static final String OOZIE_DATABASE_USER = "oozie_database_user";

    private static final String OOZIE_DATABASE_PASSWORD = "oozie_database_password";

    private static final String OOZIE_CONFIG_SAFETY_VALVE = "oozie_config_safety_valve";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case OozieRoles.OOZIE_SERVER:
                RdsView oozieRdsView = getRdsView(source);
                List<ApiClusterTemplateConfig> config = new ArrayList<>(List.of(
                    config(OOZIE_DATABASE_HOST, oozieRdsView.getHost()),
                    config(OOZIE_DATABASE_NAME, oozieRdsView.getDatabaseName()),
                    config(OOZIE_DATABASE_TYPE, oozieRdsView.getSubprotocol()),
                    config(OOZIE_DATABASE_USER, oozieRdsView.getConnectionUserName()),
                    config(OOZIE_DATABASE_PASSWORD, oozieRdsView.getConnectionPassword())));
                return config;
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return OozieRoles.OOZIE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(OozieRoles.OOZIE_SERVER);
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.OOZIE;
    }

    public static boolean isOozieHA(TemplatePreparationObject source) {
        String cdhVersion = StringUtils.defaultString(source.getBlueprintView().getProcessor().getStackVersion());

        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_2_11)) {
            return source.getHostGroupsWithComponent(OozieRoles.OOZIE_SERVER)
                .filter(hg -> hg.getNodeCount() > 1)
                .mapToInt(HostgroupView::getNodeCount)
                .sum() > 1;
        }
        return false;
    }
}

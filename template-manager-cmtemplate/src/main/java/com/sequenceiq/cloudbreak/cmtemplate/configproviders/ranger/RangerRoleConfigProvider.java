package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_6_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getCmVersion;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class RangerRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    @VisibleForTesting
    static final String RANGER_DATABASE_HOST = "ranger_database_host";

    @VisibleForTesting
    static final String RANGER_DATABASE_NAME = "ranger_database_name";

    @VisibleForTesting
    static final String RANGER_DATABASE_TYPE = "ranger_database_type";

    @VisibleForTesting
    static final String RANGER_DATABASE_USER = "ranger_database_user";

    @VisibleForTesting
    static final String RANGER_DATABASE_PASSWORD = "ranger_database_password";

    @VisibleForTesting
    static final String RANGER_ADMIN_SITE_XML_ROLE_SAFETY_VALVE = "conf/ranger-admin-site.xml_role_safety_valve";

    @VisibleForTesting
    static final String RANGER_DATABASE_PORT = "ranger_database_port";

    @VisibleForTesting
    static final String RANGER_DEFAULT_POLICY_GROUPS = "ranger.default.policy.groups";

    @VisibleForTesting
    static final String RANGER_HBASE_ADMIN_VIRTUAL_GROUPS = "ranger.hbase.default.admin.groups";

    private static final String RANGER_JPA_JDBC_URL = "ranger.jpa.jdbc.url";

    @Inject
    private VirtualGroupService virtualGroupService;

    @Override
    public String dbUserKey() {
        return RANGER_DATABASE_USER;
    }

    @Override
    public String dbPasswordKey() {
        return RANGER_DATABASE_PASSWORD;
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        String cmVersion = getCmVersion(source);
        List<ApiClusterTemplateConfig> configList = new ArrayList<>();

        if (isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_1)) {
            RdsView rangerRdsView = getRdsView(source);
            addDbConfigs(rangerRdsView, configList, cmVersion);
            configList.add(config(RANGER_DATABASE_PORT, rangerRdsView.getPort()));
        }

        return configList;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        switch (roleType) {
            case RangerRoles.RANGER_ADMIN:
                String cmVersion = getCmVersion(source);
                List<ApiClusterTemplateConfig> configList = new ArrayList<>();

                // In CM 7.2.1 and above, the ranger database parameters have moved to the service
                // config (see above getServiceConfigs).
                RdsView rangerRdsView = getRdsView(source);
                if (!isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_1)) {
                    addDbConfigs(rangerRdsView, configList, cmVersion);
                }
                addDbSslConfigsIfNeeded(rangerRdsView, configList, cmVersion);

                VirtualGroupRequest virtualGroupRequest = source.getVirtualGroupRequest();

                if (isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_0_1)) {
                    String adminGroup = virtualGroupService.createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.RANGER_ADMIN);
                    configList.add(config(RANGER_DEFAULT_POLICY_GROUPS, adminGroup));
                }

                if (isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_6_0)) {
                    String hbaseAdminGroup = virtualGroupService.createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.HBASE_ADMIN);
                    configList.add(config(RANGER_HBASE_ADMIN_VIRTUAL_GROUPS, hbaseAdminGroup));
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
    public DatabaseType dbType() {
        return DatabaseType.RANGER;
    }

    // There is a bug in CM (OPSAPS-56992) which makes the db names case-sensitive.
    // To workaround this, we have to send the correctly cased ranger_database_type depending
    // on the version of CM.
    private String versionCorrectedPostgresString(final String cmVersion) {
        return isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_1) ? "postgresql" : "PostgreSQL";
    }

    private String getRangerDbType(RdsView rangerRdsView, String cmVersion) {
        switch (rangerRdsView.getDatabaseVendor()) {
            case POSTGRES:
                return versionCorrectedPostgresString(cmVersion);
            default:
                throw new CloudbreakServiceException("Unsupported Ranger database type: " + rangerRdsView.getDatabaseVendor().displayName());
        }
    }

    private void addDbConfigs(RdsView rangerRdsView, List<ApiClusterTemplateConfig> configList, final String cmVersion) {
        configList.add(config(RANGER_DATABASE_HOST, rangerRdsView.getHost()));
        configList.add(config(RANGER_DATABASE_NAME, rangerRdsView.getDatabaseName()));
        configList.add(config(RANGER_DATABASE_TYPE, getRangerDbType(rangerRdsView, cmVersion)));
        configList.add(config(RANGER_DATABASE_USER, rangerRdsView.getConnectionUserName()));
        configList.add(config(RANGER_DATABASE_PASSWORD, rangerRdsView.getConnectionPassword()));
    }

    private void addDbSslConfigsIfNeeded(RdsView rangerRdsView, List<ApiClusterTemplateConfig> configList, String cmVersion) {
        if (isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_2) && rangerRdsView.isUseSsl()) {
            configList.add(config(RANGER_ADMIN_SITE_XML_ROLE_SAFETY_VALVE, getSafetyValveProperty(RANGER_JPA_JDBC_URL, rangerRdsView.getConnectionURL())));
        }
    }

}

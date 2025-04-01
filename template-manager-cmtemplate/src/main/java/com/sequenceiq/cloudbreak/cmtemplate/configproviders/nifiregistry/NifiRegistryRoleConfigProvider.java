package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifiregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionEqualToLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionOlderThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.cfm.CfmUtil.getCfmProduct;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.SSLModeProviderService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class NifiRegistryRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    static final String DATABASE_USER = "nifi.registry.db.username";

    static final String DATABASE_PASSWORD = "nifi.registry.db.password";

    static final String DATABASE_URL = "nifi.registry.db.url";

    static final String DRIVER_CLASS = "nifi.registry.db.driver.class";

    static final String DRIVER_DIRECTORY = "nifi.registry.db.driver.directory";

    @Override
    public String dbUserKey() {
        return DATABASE_USER;
    }

    @Override
    public String dbPasswordKey() {
        return DATABASE_PASSWORD;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_0_1)) {
            return switch (roleType) {
                case NifiRegistryRoles.NIFI_REGISTRY_SERVER -> generateDatabaseConfig(source);
                default -> List.of();
            };
        }
        return List.of();
    }

    private List<ApiClusterTemplateConfig> generateDatabaseConfig(TemplatePreparationObject source) {
        RdsView nifiRegistryRdsView = getRdsView(source);
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();

        configs.add(config(DRIVER_CLASS, nifiRegistryRdsView.getConnectionDriver()));
        configs.add(config(DATABASE_USER, nifiRegistryRdsView.getConnectionUserName()));
        configs.add(config(DATABASE_PASSWORD, nifiRegistryRdsView.getConnectionPassword()));

        Optional<ClouderaManagerProduct> cfm = getCfmProduct(source);
        String connectionURL = nifiRegistryRdsView.getConnectionURL();
        if (cfm.isEmpty() || (isVersionOlderThanLimited(cfm.get().getVersion(), CMRepositoryVersionUtil.CFM_VERSION_2_2_6_200)
                && !isVersionEqualToLimited(cfm.get().getVersion(), CMRepositoryVersionUtil.CFM_VERSION_2_2_5_300))) {
            configs.add(config(DRIVER_DIRECTORY, "/usr/share/java/"));
            if (nifiRegistryRdsView.isUseSsl()) {
                String targetString = SSLModeProviderService.getSslModeBasedOnConnectionString(connectionURL);
                connectionURL = connectionURL.replace(targetString, "sslmode=require");
            }
        }
        configs.add(config(DATABASE_URL, connectionURL));

        return unmodifiableList(configs);
    }

    @Override
    public String getServiceType() {
        return NifiRegistryRoles.NIFIREGISTRY;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(NifiRegistryRoles.NIFI_REGISTRY_SERVER);
    }

    @Override
    public DatabaseType dbType() {
        return DatabaseType.NIFIREGISTRY;
    }

}
